// zip.mjs — reads and writes the .zip a backup downloads as. Self-hosted rather than a library from a
// CDN: the Backup & Restore page holds the service-account key, so it loads no third-party code it
// does not already need. Compression is the browser's own deflate (CompressionStream).
//
// Covers what a backup needs: files under 4 GB, stored or deflated, which includes the zip Windows'
// "Compress to ZIP file" makes from a backup folder. No zip64, no encryption.

const CRC_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
    table[n] = c >>> 0;
  }
  return table;
})();

export function crc32(bytes) {
  let c = 0xFFFFFFFF;
  for (let i = 0; i < bytes.length; i++) c = CRC_TABLE[(c ^ bytes[i]) & 0xFF] ^ (c >>> 8);
  return (c ^ 0xFFFFFFFF) >>> 0;
}

async function pipe(bytes, stream) {
  const out = new Blob([bytes]).stream().pipeThrough(stream);
  return new Uint8Array(await new Response(out).arrayBuffer());
}

function dosDateTime(date) {
  return {
    time: (date.getHours() << 11) | (date.getMinutes() << 5) | (date.getSeconds() >> 1),
    day: ((Math.max(date.getFullYear(), 1980) - 1980) << 9) | ((date.getMonth() + 1) << 5) | date.getDate()
  };
}

const UTF8_NAMES = 0x0800;

/** `files` are { name, data } with data a string or bytes; `/` in a name makes a folder. */
export async function createZip(files, date = new Date()) {
  const encoder = new TextEncoder();
  const canDeflate = typeof CompressionStream === 'function';
  const { time, day } = dosDateTime(date);
  const parts = [];
  const central = [];
  let offset = 0;

  for (const file of files) {
    const name = encoder.encode(file.name);
    const data = typeof file.data === 'string' ? encoder.encode(file.data) : file.data;
    const crc = crc32(data);
    let body = data;
    let method = 0;
    if (canDeflate) {
      const packed = await pipe(data, new CompressionStream('deflate-raw'));
      if (packed.length < data.length) { body = packed; method = 8; }
    }

    const local = new DataView(new ArrayBuffer(30));
    local.setUint32(0, 0x04034b50, true);
    local.setUint16(4, 20, true);
    local.setUint16(6, UTF8_NAMES, true);
    local.setUint16(8, method, true);
    local.setUint16(10, time, true);
    local.setUint16(12, day, true);
    local.setUint32(14, crc, true);
    local.setUint32(18, body.length, true);
    local.setUint32(22, data.length, true);
    local.setUint16(26, name.length, true);
    parts.push(local.buffer, name, body);

    const entry = new DataView(new ArrayBuffer(46));
    entry.setUint32(0, 0x02014b50, true);
    entry.setUint16(4, 20, true);
    entry.setUint16(6, 20, true);
    entry.setUint16(8, UTF8_NAMES, true);
    entry.setUint16(10, method, true);
    entry.setUint16(12, time, true);
    entry.setUint16(14, day, true);
    entry.setUint32(16, crc, true);
    entry.setUint32(20, body.length, true);
    entry.setUint32(24, data.length, true);
    entry.setUint16(28, name.length, true);
    entry.setUint32(42, offset, true);
    central.push(entry.buffer, name);

    offset += 30 + name.length + body.length;
  }

  const centralSize = central.reduce((n, part) => n + part.byteLength, 0);
  const end = new DataView(new ArrayBuffer(22));
  end.setUint32(0, 0x06054b50, true);
  end.setUint16(8, files.length, true);
  end.setUint16(10, files.length, true);
  end.setUint32(12, centralSize, true);
  end.setUint32(16, offset, true);
  return new Blob([...parts, ...central, end.buffer], { type: 'application/zip' });
}

const DAMAGED = 'This zip is damaged or is not a zip file.';

/**
 * The files in a zip, as { name, data } with data as bytes. Folders are left out; a name keeps its
 * folder path with `/` separators. `want(name)` skips files that are not needed before inflating.
 */
export async function readZip(buffer, want = () => true) {
  const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer);
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  let end = -1;
  for (let p = bytes.length - 22; p >= Math.max(0, bytes.length - 22 - 0xFFFF); p--) {
    if (view.getUint32(p, true) === 0x06054b50) { end = p; break; }
  }
  if (end < 0) throw new Error(DAMAGED);
  const count = view.getUint16(end + 10, true);
  let p = view.getUint32(end + 16, true);
  if (count === 0xFFFF || p === 0xFFFFFFFF) throw new Error('This zip is over 4 GB, too large for a backup.');

  const decoder = new TextDecoder();
  const out = [];
  for (let i = 0; i < count; i++) {
    if (p + 46 > bytes.length || view.getUint32(p, true) !== 0x02014b50) throw new Error(DAMAGED);
    const flags = view.getUint16(p + 8, true);
    const method = view.getUint16(p + 10, true);
    const crc = view.getUint32(p + 16, true);
    const packedSize = view.getUint32(p + 20, true);
    const nameLength = view.getUint16(p + 28, true);
    const extraLength = view.getUint16(p + 30, true);
    const commentLength = view.getUint16(p + 32, true);
    const localOffset = view.getUint32(p + 42, true);
    const name = decoder.decode(bytes.subarray(p + 46, p + 46 + nameLength)).replace(/\\/g, '/');
    p += 46 + nameLength + extraLength + commentLength;

    if (name.endsWith('/') || !want(name)) continue;
    if (flags & 1) throw new Error('This zip is password-protected. Zip the backup folder again without a password.');
    if (localOffset + 30 > bytes.length || view.getUint32(localOffset, true) !== 0x04034b50) throw new Error(DAMAGED);
    const start = localOffset + 30 + view.getUint16(localOffset + 26, true) + view.getUint16(localOffset + 28, true);
    const packed = bytes.subarray(start, start + packedSize);
    let data;
    if (method === 0) data = packed;
    else if (method === 8) data = await pipe(packed, new DecompressionStream('deflate-raw'));
    else throw new Error(`${name} uses a compression this page cannot read. Zip the backup folder with Windows instead.`);
    if (crc32(data) !== crc) throw new Error(`${name} in this zip is damaged.`);
    out.push({ name, data });
  }
  return out;
}
