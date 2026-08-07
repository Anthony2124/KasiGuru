package com.kasiguru.ui.theme

import io.eyram.iconsax.IconSax

/**
 * Iconsax icon registry for the entire KasiGuru app.
 * All icons use the real iconsax-android library (io.eyram.iconsax) Bulk style.
 * Use: painterResource(Iconsax.XxxName) in Icon() composables.
 */
object Iconsax {

    // ── Bottom Navigation ────────────────────────────────────────────
    val HomeOutline: Int get() = IconSax.Bulk.Home
    val HomeBold: Int get() = IconSax.Bulk.Home
    val BookOutline: Int get() = IconSax.Bulk.Book
    val BookBold: Int get() = IconSax.Bulk.Book
    val RepeatOutline: Int get() = IconSax.Bulk.Repeat
    val RepeatBold: Int get() = IconSax.Bulk.Repeat
    val Element4Outline: Int get() = IconSax.Bulk.Element4
    val Element4Bold: Int get() = IconSax.Bulk.Element4
    val ProfileOutline: Int get() = IconSax.Bulk.ProfileCircle
    val ProfileBold: Int get() = IconSax.Bulk.ProfileCircle

    // ── Navigation Arrows ────────────────────────────────────────────
    val ArrowLeft: Int get() = IconSax.Bulk.ArrowLeft
    val ArrowRight: Int get() = IconSax.Bulk.ArrowRight
    val ArrowUp: Int get() = IconSax.Bulk.ArrowUp
    val ArrowDown: Int get() = IconSax.Bulk.ArrowDown

    // ── User / Profile ───────────────────────────────────────────────
    val Profile: Int get() = IconSax.Bulk.ProfileCircle
    val ProfileBoldIcon: Int get() = IconSax.Bulk.ProfileCircle
    val People: Int get() = IconSax.Bulk.People
    val Profile2user: Int get() = IconSax.Bulk.Profile2user

    // ── Communication ────────────────────────────────────────────────
    val Sms: Int get() = IconSax.Bulk.Sms
    val Notification: Int get() = IconSax.Bulk.Notification
    val NotificationBold: Int get() = IconSax.Bulk.Notification

    // ── Content / Books ──────────────────────────────────────────────
    val Book: Int get() = IconSax.Bulk.Book
    val BookBoldIcon: Int get() = IconSax.Bulk.Book
    val Document: Int get() = IconSax.Bulk.Document
    val Teacher: Int get() = IconSax.Bulk.Teacher

    // ── Gamification / Achievements ──────────────────────────────────
    val Cup: Int get() = IconSax.Bulk.Cup
    val CupBold: Int get() = IconSax.Bulk.Cup
    val Medal: Int get() = IconSax.Bulk.Medal
    val MedalStar: Int get() = IconSax.Bulk.MedalStar
    val MedalStarBold: Int get() = IconSax.Bulk.MedalStar
    val Star: Int get() = IconSax.Bulk.Star1
    val StarBold: Int get() = IconSax.Bulk.Star1
    val Flash: Int get() = IconSax.Bulk.Flash
    val FlashBold: Int get() = IconSax.Bulk.Flash
    val Flash1: Int get() = IconSax.Bulk.Flash
    val Trophy: Int get() = IconSax.Bulk.Cup
    val TrophyBold: Int get() = IconSax.Bulk.Cup

    // ── Media / Audio ────────────────────────────────────────────────
    val VolumeHigh: Int get() = IconSax.Bulk.VolumeHigh
    val VolumeHighBold: Int get() = IconSax.Bulk.VolumeHigh
    val VolumeUp: Int get() = IconSax.Bulk.VolumeUp
    val Play: Int get() = IconSax.Bulk.Play
    val PlayBold: Int get() = IconSax.Bulk.Play
    val PlayCircle: Int get() = IconSax.Bulk.PlayCircle

    // ── UI Actions ───────────────────────────────────────────────────
    val Edit: Int get() = IconSax.Bulk.Edit
    val EditBold: Int get() = IconSax.Bulk.Edit
    val Search: Int get() = IconSax.Bulk.SearchNormal
    val SearchBold: Int get() = IconSax.Bulk.SearchNormal
    val Setting: Int get() = IconSax.Bulk.Setting2
    val SettingBold: Int get() = IconSax.Bulk.Setting2
    val Refresh: Int get() = IconSax.Bulk.Refresh
    val Logout: Int get() = IconSax.Bulk.Logout

    // ── Status / Feedback ────────────────────────────────────────────
    val TickCircle: Int get() = IconSax.Bulk.TickCircle
    val TickCircleBold: Int get() = IconSax.Bulk.TickCircle
    val TickSquare: Int get() = IconSax.Bulk.TickSquare
    val Add: Int get() = IconSax.Bulk.AddCircle
    val AddCircle: Int get() = IconSax.Bulk.AddCircle
    val InfoCircle: Int get() = IconSax.Bulk.InfoCircle
    val Global: Int get() = IconSax.Bulk.Global

    // ── Security ────────────────────────────────────────────────────
    val Lock: Int get() = IconSax.Bulk.Lock
    val LockBold: Int get() = IconSax.Bulk.Lock

    // ── Time / Date ──────────────────────────────────────────────────
    val Calendar: Int get() = IconSax.Bulk.Calendar

    // ── Location ─────────────────────────────────────────────────────
    val Location: Int get() = IconSax.Bulk.Location

    // ── Theme / Display ──────────────────────────────────────────────
    val Moon: Int get() = IconSax.Bulk.Moon

    // ── Heritage / Cultural ──────────────────────────────────────────
    val Courthouse: Int get() = IconSax.Bulk.Courthouse
    val Game: Int get() = IconSax.Bulk.Game
    val GameBold: Int get() = IconSax.Bulk.Game

    // ── Expand / Collapse ────────────────────────────────────────────
    val ArrowUp1: Int get() = IconSax.Bulk.ArrowUp
    val ArrowDown1: Int get() = IconSax.Bulk.ArrowDown

    // ── Repeat ───────────────────────────────────────────────────────
    val Repeat: Int get() = IconSax.Bulk.Repeat
    val RepeatIcon: Int get() = IconSax.Bulk.Repeat
}
