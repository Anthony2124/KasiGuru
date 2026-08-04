package com.kasiguru.ui.theme

import io.eyram.iconsax.IconSax

/**
 * Iconsax icon registry for the entire KasiGuru app.
 * All icons use the real iconsax-android library (io.eyram.iconsax).
 * Use: painterResource(Iconsax.XxxName) in Icon() composables.
 */
object Iconsax {

    // ── Bottom Navigation ────────────────────────────────────────────
    val HomeOutline: Int get() = IconSax.Outline.Home
    val HomeBold: Int get() = IconSax.Bold.Home
    val BookOutline: Int get() = IconSax.Outline.Book
    val BookBold: Int get() = IconSax.Bold.Book
    val RepeatOutline: Int get() = IconSax.Outline.Repeat
    val RepeatBold: Int get() = IconSax.Bold.Repeat
    val Element4Outline: Int get() = IconSax.Outline.Element4
    val Element4Bold: Int get() = IconSax.Bold.Element4
    val ProfileOutline: Int get() = IconSax.Outline.ProfileCircle
    val ProfileBold: Int get() = IconSax.Bold.ProfileCircle

    // ── Navigation Arrows ────────────────────────────────────────────
    val ArrowLeft: Int get() = IconSax.Outline.ArrowLeft
    val ArrowRight: Int get() = IconSax.Outline.ArrowRight
    val ArrowUp: Int get() = IconSax.Outline.ArrowUp
    val ArrowDown: Int get() = IconSax.Outline.ArrowDown

    // ── User / Profile ───────────────────────────────────────────────
    val Profile: Int get() = IconSax.Outline.ProfileCircle
    val ProfileBoldIcon: Int get() = IconSax.Bold.ProfileCircle
    val People: Int get() = IconSax.Outline.People
    val Profile2user: Int get() = IconSax.Outline.Profile2user

    // ── Communication ────────────────────────────────────────────────
    val Sms: Int get() = IconSax.Outline.Sms
    val Notification: Int get() = IconSax.Outline.Notification
    val NotificationBold: Int get() = IconSax.Bold.Notification

    // ── Content / Books ──────────────────────────────────────────────
    val Book: Int get() = IconSax.Outline.Book
    val BookBoldIcon: Int get() = IconSax.Bold.Book
    val Document: Int get() = IconSax.Outline.Document
    val Teacher: Int get() = IconSax.Outline.Teacher

    // ── Gamification / Achievements ──────────────────────────────────
    val Cup: Int get() = IconSax.Outline.Cup
    val CupBold: Int get() = IconSax.Bold.Cup
    val Medal: Int get() = IconSax.Outline.Medal
    val MedalStar: Int get() = IconSax.Outline.MedalStar
    val MedalStarBold: Int get() = IconSax.Bold.MedalStar
    val Star: Int get() = IconSax.Outline.Star1
    val StarBold: Int get() = IconSax.Bold.Star1
    val Flash: Int get() = IconSax.Outline.Flash
    val FlashBold: Int get() = IconSax.Bold.Flash
    val Flash1: Int get() = IconSax.Outline.Flash1
    val Trophy: Int get() = IconSax.Outline.Cup
    val TrophyBold: Int get() = IconSax.Bold.Cup

    // ── Media / Audio ────────────────────────────────────────────────
    val VolumeHigh: Int get() = IconSax.Outline.VolumeHigh
    val VolumeHighBold: Int get() = IconSax.Bold.VolumeHigh
    val VolumeUp: Int get() = IconSax.Outline.VolumeUp
    val Play: Int get() = IconSax.Outline.Play
    val PlayBold: Int get() = IconSax.Bold.Play
    val PlayCircle: Int get() = IconSax.Outline.PlayCircle

    // ── UI Actions ───────────────────────────────────────────────────
    val Edit: Int get() = IconSax.Outline.Edit
    val EditBold: Int get() = IconSax.Bold.Edit
    val Search: Int get() = IconSax.Outline.SearchNormal
    val SearchBold: Int get() = IconSax.Bold.SearchNormal
    val Setting: Int get() = IconSax.Outline.Setting2
    val SettingBold: Int get() = IconSax.Bold.Setting2
    val Refresh: Int get() = IconSax.Outline.Refresh
    val Logout: Int get() = IconSax.Outline.Logout

    // ── Status / Feedback ────────────────────────────────────────────
    val TickCircle: Int get() = IconSax.Outline.TickCircle
    val TickCircleBold: Int get() = IconSax.Bold.TickCircle
    val TickSquare: Int get() = IconSax.Outline.TickSquare
    val InfoCircle: Int get() = IconSax.Outline.InfoCircle
    val Global: Int get() = IconSax.Outline.Global

    // ── Security ────────────────────────────────────────────────────
    val Lock: Int get() = IconSax.Outline.Lock
    val LockBold: Int get() = IconSax.Bold.Lock

    // ── Time / Date ──────────────────────────────────────────────────
    val Calendar: Int get() = IconSax.Outline.Calendar

    // ── Location ─────────────────────────────────────────────────────
    val Location: Int get() = IconSax.Outline.Location

    // ── Theme / Display ──────────────────────────────────────────────
    val Moon: Int get() = IconSax.Outline.Moon

    // ── Heritage / Cultural ──────────────────────────────────────────
    val Courthouse: Int get() = IconSax.Outline.Courthouse
    val Game: Int get() = IconSax.Outline.Game
    val GameBold: Int get() = IconSax.Bold.Game

    // ── Expand / Collapse ────────────────────────────────────────────
    val ArrowUp1: Int get() = IconSax.Outline.ArrowUp1
    val ArrowDown1: Int get() = IconSax.Outline.ArrowDown1

    // ── Repeat ───────────────────────────────────────────────────────
    val Repeat: Int get() = IconSax.Outline.Repeat
    val RepeatIcon: Int get() = IconSax.Outline.Repeat
}
