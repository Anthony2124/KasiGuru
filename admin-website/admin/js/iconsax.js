// Official 2-Tone Iconsax Bulk SVG Vector Registry (Matches Android io.eyram.iconsax.IconSax.Bulk)
(function() {
  function getIconsaxBulkSvg(name, size, color) {
    var c = color || 'currentColor';
    
    var svgMap = {
      'eye': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M2 12C4.2 7.6 7.8 5 12 5C16.2 5 19.8 7.6 22 12C19.8 16.4 16.2 19 12 19C7.8 19 4.2 16.4 2 12Z" fill="' + c + '" opacity="0.35"/>' +
        '<circle cx="12" cy="12" r="3.25" fill="' + c + '"/>' +
      '</svg>',
      'eye-slash': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M2 12C4.2 7.6 7.8 5 12 5C16.2 5 19.8 7.6 22 12C19.8 16.4 16.2 19 12 19C7.8 19 4.2 16.4 2 12Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M4 4L20 20" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'lock-1': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<rect x="4" y="10" width="16" height="11" rx="4" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M8 10V7.5C8 5.29 9.79 3.5 12 3.5C14.21 3.5 16 5.29 16 7.5V10" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
        '<path d="M12 14.5V16.5" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'copy': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<rect x="8.5" y="8.5" width="12" height="12" rx="3" fill="' + c + '"/>' +
        '<path d="M15.5 8.5V6.5C15.5 4.84 14.16 3.5 12.5 3.5H6.5C4.84 3.5 3.5 4.84 3.5 6.5V12.5C3.5 14.16 4.84 15.5 6.5 15.5H8.5" fill="' + c + '" opacity="0.35"/>' +
      '</svg>',
      'menu-1': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M3 7H21M3 12H21M3 17H21" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'arrow-left': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M15 5L8 12L15 19" stroke="' + c + '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>' +
      '</svg>',
      'arrow-right': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M9 5L16 12L9 19" stroke="' + c + '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>' +
      '</svg>',
      'people': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="9" cy="8" r="3.5" fill="' + c + '"/>' +
        '<path d="M2.5 19.5C2.5 16.5 5.4 14.5 9 14.5C12.6 14.5 15.5 16.5 15.5 19.5V20.5H2.5V19.5Z" fill="' + c + '" opacity="0.35"/>' +
        '<circle cx="16.5" cy="8.5" r="2.75" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M17 14.6C19.6 14.9 21.5 16.6 21.5 19V20.5H17.5" stroke="' + c + '" stroke-width="1.8" stroke-linecap="round"/>' +
      '</svg>',
      'profile-2user': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="9" cy="8" r="3.5" fill="' + c + '"/>' +
        '<path d="M2.5 19.5C2.5 16.5 5.4 14.5 9 14.5C12.6 14.5 15.5 16.5 15.5 19.5V20.5H2.5V19.5Z" fill="' + c + '" opacity="0.35"/>' +
        '<circle cx="16.5" cy="8.5" r="2.75" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M17 14.6C19.6 14.9 21.5 16.6 21.5 19V20.5H17.5" stroke="' + c + '" stroke-width="1.8" stroke-linecap="round"/>' +
      '</svg>',
      'profile-circle': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="12" cy="12" r="9.5" fill="' + c + '" opacity="0.35"/>' +
        '<circle cx="12" cy="9.5" r="3.25" fill="' + c + '"/>' +
        '<path d="M6.5 18.3C7.7 16.3 9.7 15.2 12 15.2C14.3 15.2 16.3 16.3 17.5 18.3" stroke="' + c + '" stroke-width="1.8" stroke-linecap="round"/>' +
      '</svg>',
      'user-add': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="10" cy="7.5" r="4" fill="' + c + '"/>' +
        '<path d="M2.5 20C2.5 16.4 5.9 14 10 14C11.6 14 13.1 14.4 14.3 15V21H2.5V20Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M19 14V20M16 17H22" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'hierarchy-square-2': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<rect x="8.5" y="2.5" width="7" height="6" rx="2" fill="' + c + '"/>' +
        '<rect x="2.5" y="15.5" width="7" height="6" rx="2" fill="' + c + '" opacity="0.35"/>' +
        '<rect x="14.5" y="15.5" width="7" height="6" rx="2" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M12 8.5V12M6 15.5V12H18V15.5" stroke="' + c + '" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>' +
      '</svg>',
      'shield-tick': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M12 2.5L19.5 5.5V11.5C19.5 16 16.3 19.8 12 21.5C7.7 19.8 4.5 16 4.5 11.5V5.5L12 2.5Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M8.5 12L11 14.5L15.5 9.5" stroke="' + c + '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>' +
      '</svg>',
      'shield-cross': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M12 2.5L19.5 5.5V11.5C19.5 16 16.3 19.8 12 21.5C7.7 19.8 4.5 16 4.5 11.5V5.5L12 2.5Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M9.5 9.5L14.5 14.5M14.5 9.5L9.5 14.5" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'cloud-change': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M6.5 18.5C4 18.5 2 16.5 2 14C2 11.7 3.7 9.8 5.9 9.5C6.6 6.6 9.1 4.5 12 4.5C15.1 4.5 17.7 6.9 18 10C20.3 10.2 22 12.1 22 14.3C22 16.6 20.1 18.5 17.8 18.5H6.5Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M9 13.5L12 10.5L15 13.5M12 10.5V17" stroke="' + c + '" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>' +
      '</svg>',
      'fire': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M12 21.5C8 21.5 5 18.6 5 14.7C5 10.5 8.5 7.8 9.5 2.5C12.5 4.5 13.5 7 13.4 9.5C14.6 8.6 15.3 7.4 15.5 6C17.8 8 19 11 19 14.7C19 18.6 16 21.5 12 21.5Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M12 21.5C10.1 21.5 8.7 20.1 8.7 18.3C8.7 16.2 10.4 15.1 11 12.5C12.7 13.5 15.3 15.4 15.3 18.3C15.3 20.1 13.9 21.5 12 21.5Z" fill="' + c + '"/>' +
      '</svg>',
      'sms': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<rect x="2" y="4" width="20" height="16" rx="4" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M3 8L10.2 12.8C11.3 13.5 12.7 13.5 13.8 12.8L21 8" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'lock': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<rect x="4" y="10" width="16" height="11" rx="4" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M8 10V7.5C8 5.29 9.79 3.5 12 3.5C14.21 3.5 16 5.29 16 7.5V10" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
        '<circle cx="12" cy="15.5" r="1.75" fill="' + c + '"/>' +
      '</svg>',
      'user': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="12" cy="7.5" r="4" fill="' + c + '"/>' +
        '<path d="M4 20C4 16.4 7.6 14 12 14C16.4 14 20 16.4 20 20V21H4V20Z" fill="' + c + '" opacity="0.35"/>' +
      '</svg>',
      'edit': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M4 20H8L19 9L15 5L4 16V20Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M15 5L19 9" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
        '<path d="M17 3L21 7" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'logout': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M4 5C4 3.9 4.9 3 6 3H11V21H6C4.9 21 4 20.1 4 19V5Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M16 8L20 12L16 16" stroke="' + c + '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>' +
        '<path d="M20 12H10" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'login': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M13 3H18C19.1 3 20 3.9 20 5V19C20 20.1 19.1 21 18 21H13V3Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M8 8L4 12L8 16" stroke="' + c + '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>' +
        '<path d="M4 12H14" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'info-circle': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="12" cy="12" r="10" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M12 11V16" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
        '<circle cx="12" cy="7.75" r="1.15" fill="' + c + '"/>' +
      '</svg>',
      'document-text': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M5 4C5 2.9 5.9 2 7 2H14L19 7V20C19 21.1 18.1 22 17 22H7C5.9 22 5 21.1 5 20V4Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M8.5 12H15.5M8.5 16H13" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'security-safe': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M12 2L20 5.5V11C20 16 16.5 20.3 12 22C7.5 20.3 4 16 4 11V5.5L12 2Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M8.75 12L11.25 14.5L15.5 10" stroke="' + c + '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>' +
      '</svg>',
      'element-3': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<rect x="3" y="3" width="8" height="8" rx="3" fill="' + c + '" opacity="0.35"/>' +
        '<rect x="13" y="3" width="8" height="8" rx="3" fill="' + c + '"/>' +
        '<rect x="3" y="13" width="8" height="8" rx="3" fill="' + c + '"/>' +
        '<rect x="13" y="13" width="8" height="8" rx="3" fill="' + c + '" opacity="0.35"/>' +
      '</svg>',
      'clock': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="12" cy="12" r="9" fill="' + c + '" opacity="0.3"/>' +
        '<path d="M12 7V12L15 15" stroke="' + c + '" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>' +
        '<circle cx="12" cy="12" r="2.5" fill="' + c + '"/>' +
      '</svg>',
      'book-1': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M4 4.5C4 3.12 5.12 2 6.5 2H12V22H6.5C5.12 22 4 20.88 4 19.5V4.5Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M20 4.5C20 3.12 18.88 2 17.5 2H12V22H17.5C18.88 22 20 20.88 20 19.5V4.5Z" fill="' + c + '"/>' +
        '<path d="M7 6H10M7 10H10" stroke="#12161F" stroke-width="1.5" stroke-linecap="round"/>' +
      '</svg>',
      'box-search': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M12 2L3 7V17L12 22L21 17V7L12 2Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M12 22V12L21 7M12 12L3 7" stroke="' + c + '" stroke-width="2"/>' +
        '<circle cx="11" cy="11" r="3" fill="' + c + '"/>' +
      '</svg>',
      'mobile': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<rect x="5" y="2" width="14" height="20" rx="4" fill="' + c + '" opacity="0.35"/>' +
        '<rect x="7" y="5" width="10" height="11" rx="2" fill="' + c + '"/>' +
        '<circle cx="12" cy="18.5" r="1.5" fill="' + c + '"/>' +
      '</svg>',
      'flash-1': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M13 2L3 14H12L11 22L21 10H12L13 2Z" fill="' + c + '"/>' +
        '<path d="M13 2L3 14H12L13 2Z" fill="' + c + '" opacity="0.4"/>' +
      '</svg>',
      'grid-5': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<rect x="3" y="3" width="8" height="18" rx="3" fill="' + c + '" opacity="0.35"/>' +
        '<rect x="13" y="3" width="8" height="8" rx="3" fill="' + c + '"/>' +
        '<rect x="13" y="13" width="8" height="8" rx="3" fill="' + c + '"/>' +
      '</svg>',
      'cup': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M5 3H19V11C19 14.87 15.87 18 12 18C8.13 18 5 14.87 5 11V3Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M8 21H16M12 18V21" stroke="' + c + '" stroke-width="2.5" stroke-linecap="round"/>' +
        '<path d="M12 6V12M9 9H15" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'add-circle': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="12" cy="12" r="9" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M12 8V16M8 12H16" stroke="' + c + '" stroke-width="2.5" stroke-linecap="round"/>' +
      '</svg>',
      'verify': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="12" cy="12" r="9" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M8.5 12.5L10.5 14.5L15.5 9.5" stroke="' + c + '" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>' +
      '</svg>',
      'export': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M13 5L20 12L13 19M20 12H8" stroke="' + c + '" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>' +
        '<path d="M4 4V20" stroke="' + c + '" stroke-width="2.5" stroke-linecap="round" opacity="0.35"/>' +
      '</svg>',
      'document-upload': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<rect x="4" y="2" width="16" height="20" rx="4" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M12 16V8M12 8L9 11M12 8L15 11" stroke="' + c + '" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>' +
      '</svg>',
      'document-download': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<rect x="4" y="2" width="16" height="20" rx="4" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M12 8V16M12 16L9 13M12 16L15 13" stroke="' + c + '" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>' +
      '</svg>',
      'star-1': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" fill="' + c + '"/>' +
        '<path d="M12 2L15.09 8.26L22 9.27L17 14.14L12 17.77V2Z" fill="' + c + '" opacity="0.4"/>' +
      '</svg>',
      'game': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<rect x="2" y="6" width="20" height="12" rx="6" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M6 12H10M8 10V14" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
        '<circle cx="15" cy="11" r="1.2" fill="' + c + '"/>' +
        '<circle cx="17.5" cy="13" r="1.2" fill="' + c + '"/>' +
      '</svg>',
      'heart': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M12 21.35L10.55 20.03C5.4 15.36 2 12.28 2 8.5C2 5.42 4.42 3 7.5 3C9.24 3 10.91 3.81 12 5.09C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.42 22 8.5C22 12.28 18.6 15.36 13.45 20.04L12 21.35Z" fill="' + c + '"/>' +
        '<path d="M12 21.35L13.45 20.04C18.6 15.36 22 12.28 22 8.5C22 5.42 19.58 3 16.5 3C14.76 3 13.09 3.81 12 5.09V21.35Z" fill="' + c + '" opacity="0.4"/>' +
      '</svg>',
      'calendar-1': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<rect x="3" y="4" width="18" height="17" rx="4" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M3 9H21" stroke="' + c + '" stroke-width="2"/>' +
        '<path d="M8 2V5M16 2V5" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'send-1': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M22 2L11 13M22 2L15 22L11 13M22 2L2 9L11 13" stroke="' + c + '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>' +
        '<path d="M22 2L11 13" fill="' + c + '" opacity="0.4"/>' +
      '</svg>',
      'security-user': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="12" cy="8" r="4" fill="' + c + '"/>' +
        '<path d="M4 20C4 16.69 7.58 14 12 14C16.42 14 20 16.69 20 20" fill="' + c + '" opacity="0.35"/>' +
      '</svg>',
      'android': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M6 10V18C6 19.1 6.9 20 8 20H16C17.1 20 18 19.1 18 18V10H6Z" fill="' + c + '"/>' +
        '<path d="M12 4C8.69 4 6 6.69 6 10H18C18 6.69 15.31 4 12 4Z" fill="' + c + '" opacity="0.4"/>' +
        '<circle cx="9" cy="7" r="1" fill="#12161F"/>' +
        '<circle cx="15" cy="7" r="1" fill="#12161F"/>' +
      '</svg>',
      'search-normal-1': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="11" cy="11" r="7" fill="' + c + '" opacity="0.35"/>' +
        '<circle cx="11" cy="11" r="7" stroke="' + c + '" stroke-width="2"/>' +
        '<path d="M16.5 16.5L21 21" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'notification': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M12 2.5C8.4 2.5 5.5 5.4 5.5 9V12.6L4.2 15.6C3.9 16.3 4.4 17 5.1 17H18.9C19.6 17 20.1 16.3 19.8 15.6L18.5 12.6V9C18.5 5.4 15.6 2.5 12 2.5Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M9.5 19.5C9.9 20.6 10.9 21.4 12 21.4C13.1 21.4 14.1 20.6 14.5 19.5" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'flag': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M5.5 3.5H17.2C18.1 3.5 18.6 4.5 18.1 5.2L15.9 8.3L18.1 11.4C18.6 12.1 18.1 13.1 17.2 13.1H5.5V3.5Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M5.5 21V3" stroke="' + c + '" stroke-width="2" stroke-linecap="round"/>' +
      '</svg>',
      'arrow-up-right': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M7 17L17 7" stroke="' + c + '" stroke-width="2.2" stroke-linecap="round"/>' +
        '<path d="M9 7H17V15" stroke="' + c + '" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>' +
      '</svg>',
      'tick-circle': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="12" cy="12" r="9" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M8.5 12.5L10.5 14.5L15.5 9.5" stroke="' + c + '" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>' +
      '</svg>',
      'close-circle': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="12" cy="12" r="9" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M9 9L15 15M15 9L9 15" stroke="' + c + '" stroke-width="2.5" stroke-linecap="round"/>' +
      '</svg>',
      'trash': '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M5 7L6.5 20C6.6 20.8 7.3 21.5 8.1 21.5H15.9C16.7 21.5 17.4 20.8 17.5 20L19 7H5Z" fill="' + c + '" opacity="0.35"/>' +
        '<path d="M3 7H21M10 11V17M14 11V17M9 7V4C9 3.4 9.4 3 10 3H14C14.6 3 15 3.4 15 4V7" stroke="' + c + '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>' +
      '</svg>'
    };

    if (name === 'lock-1') { name = 'lock'; }
    return svgMap[name] || svgMap['element-3'];
  }

  class IconsaxIcon extends HTMLElement {
    connectedCallback() {
      this.render();
    }
    static get observedAttributes() {
      return ['name', 'type', 'size', 'color'];
    }
    attributeChangedCallback() {
      this.render();
    }
    render() {
      var name = this.getAttribute('name') || 'element-3';
      var size = parseInt(this.getAttribute('size')) || 20;
      var color = this.getAttribute('color') || 'currentColor';

      this.style.display = 'inline-flex';
      this.style.alignItems = 'center';
      this.style.justifyContent = 'center';
      this.style.verticalAlign = 'middle';
      this.style.width = size + 'px';
      this.style.height = size + 'px';

      this.innerHTML = getIconsaxBulkSvg(name, size, color);
    }
  }

  if (!customElements.get('iconsax-icon')) {
    customElements.define('iconsax-icon', IconsaxIcon);
  }
})();
