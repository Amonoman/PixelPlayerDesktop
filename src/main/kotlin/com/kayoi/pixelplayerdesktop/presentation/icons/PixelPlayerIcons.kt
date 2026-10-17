package com.kayoi.pixelplayerdesktop.presentation.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object PixelPlayerIcons {
    val Lyrics: ImageVector by lazy {
        ImageVector.Builder(
            name = "RoundedLyrics24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
            autoMirror = true
        ).addPath(
            pathData = PathParser().parsePathString(
                "M280,560L360,560Q377,560 388.5,548.5Q400,537 400,520Q400,503 388.5,491.5Q377,480 360,480L280,480Q263,480 251.5,491.5Q240,503 240,520Q240,537 251.5,548.5Q263,560 280,560ZM760,480Q710,480 675,445Q640,410 640,360Q640,310 675,275Q710,240 760,240Q771,240 781,242Q791,244 800,247L800,80Q800,63 811.5,51.5Q823,40 840,40L920,40Q937,40 948.5,51.5Q960,63 960,80Q960,97 948.5,108.5Q937,120 920,120L880,120L880,360Q880,410 845,445Q810,480 760,480ZM280,440L480,440Q497,440 508.5,428.5Q520,417 520,400Q520,383 508.5,371.5Q497,360 480,360L280,360Q263,360 251.5,371.5Q240,383 240,400Q240,417 251.5,428.5Q263,440 280,440ZM280,320L480,320Q497,320 508.5,308.5Q520,297 520,280Q520,263 508.5,251.5Q497,240 480,240L280,240Q263,240 251.5,251.5Q240,263 240,280Q240,297 251.5,308.5Q263,320 280,320ZM600,720L240,720L148,812Q142,818 135,821Q128,824 120,824Q104,824 92,812.5Q80,801 80,783L80,160Q80,127 103.5,103.5Q127,80 160,80L600,80Q633,80 656.5,103.5Q680,127 680,160L680,177Q656,188 636,204Q616,220 600,240L600,160Q600,160 600,160Q600,160 600,160L160,160Q160,160 160,160Q160,160 160,160L160,687L207,640L600,640Q600,640 600,640Q600,640 600,640L600,480Q616,500 636,516Q656,532 680,543L680,640Q680,673 656.5,696.5Q633,720 600,720ZM160,640L160,160Q160,160 160,160Q160,160 160,160L160,160Q160,160 160,160L160,160L160,240Q160,265 160,295.5Q160,326 160,360Q160,394 160,424.5Q160,455 160,480L160,640Q160,640 160,640Q160,640 160,640L160,640Z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).build()
    }

    val AppLogo: ImageVector by lazy {
        ImageVector.Builder(
            name = "PixelPlayerLogo",
            defaultWidth = 108.dp,
            defaultHeight = 108.dp,
            viewportWidth = 183f,
            viewportHeight = 183f
        ).addGroup(
            name = "LogoGroup",
            rotate = 0f,
            pivotX = 35.5f,
            pivotY = 45.5f,
            scaleX = 0.253f,
            scaleY = 0.253f,
            translationX = 0f,
            translationY = 0f,
            clipPathData = emptyList()
        ).addPath(
            pathData = PathParser().parsePathString(
                "M90.5,-95.8c-44.3,5.7 -84,39.5 -98,83.3 -3.9,12.4 -5.4,20.9 -6.1,35 -1.2,22 0.2,30.9 19.4,124.5 13.2,64.3 14.1,83.9 5.7,125.8 -9.3,46.7 -22.5,116.1 -23.4,123.7 -5.3,42.1 7.8,81.2 36.6,109.5 10.7,10.5 18.9,16.6 31.3,22.9 20.5,10.5 39.2,14.9 63.5,14.9 22.9,-0 38.3,-3 60.7,-11.8 18.7,-7.4 24.6,-11.3 190.8,-126.4 17.9,-12.3 48.5,-33.5 68,-46.9 45.8,-31.6 56.4,-39.6 68.1,-51.2 19.6,-19.5 31,-38.5 37,-61.5 1.9,-7.6 2.3,-11.5 2.3,-25 -0.1,-14 -0.4,-17.2 -2.6,-25.7 -3.6,-13.3 -8.6,-24.3 -16.3,-35.8 -14.5,-21.9 -30.3,-35.2 -81,-68.3l-15,-9.8 -1.2,6.5c-2.8,15.3 -12.5,25.1 -28.4,28.6 -5.5,1.2 -11.6,0.7 -19.9,-1.6 -2.4,-0.7 -2.4,-0.7 0.7,2.9 8.6,9.9 19.9,29.8 24.2,42.7 4.5,14 6.2,24 6.8,42 0.9,27.6 -2.5,45.9 -12.2,65.7 -9,18.4 -17.2,28.2 -51.9,62.6 -17.1,16.9 -34,33.1 -37.6,35.8 -7.5,5.7 -25.3,14.8 -35.3,17.8 -17.5,5.3 -41.7,7 -60.2,4.2 -36,-5.5 -61,-18.4 -83.1,-42.6 -19.4,-21.3 -30.4,-40.6 -37,-65 -2.4,-8.9 -2.7,-12.2 -3.1,-30.5 -0.4,-15.8 -0.1,-22.6 1.1,-29.6 3.3,-19.4 12,-39.3 24,-55.6 10.3,-13.8 41.1,-45.9 59.6,-61.9 14,-12.2 31.2,-22 48.7,-27.8 13.2,-4.4 23.4,-6.2 37.9,-6.8l13.1,-0.6 0.6,-19.1c0.6,-21.9 1.5,-25.8 7.9,-33.1 4.2,-4.7 12.1,-9.5 15.9,-9.5 4,-0 1.2,-2.5 -12.3,-11 -7.4,-4.7 -28.3,-18.2 -46.4,-30.1 -18.1,-11.9 -39,-25.4 -46.4,-29.9 -28.1,-17.3 -50.5,-27 -70.6,-30.5 -7.9,-1.4 -28,-1.8 -35.9,-0.8z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).addPath(
            pathData = PathParser().parsePathString(
                "M306.7,24.0c-2.2,0.7 -5,2.4 -6.3,3.6 -5.3,5 -5.4,6.7 -5.4,79 0,36.7 -0.3,67.4 -0.6,68.3 -0.9,2.2 -3.7,2 -16.9,-1.4 -29.4,-7.6 -54.9,-1.6 -74.1,17.5 -14,13.8 -21.1,29.1 -22.1,47.5 -1.3,23.8 9.7,48.1 28,61.7 13.7,10.2 28.1,14.6 47.2,14.5 35.8,-0.2 64.8,-20.2 74.2,-51.2 1.5,-4.9 1.8,-14.8 2.3,-90.9 0.5,-80.1 0.6,-85.5 2.3,-86.5 1.3,-0.9 5.8,-0 20.5,3.8 39.5,10.3 38.7,10.2 43.9,8.8 3.3,-0.9 6,-2.5 8.6,-5.2l3.7,-3.9 0,-17.5c0,-17.3 0,-17.5 -2.6,-20.5 -1.4,-1.7 -4.2,-3.5 -6.2,-4.1 -2,-0.5 -15.2,-4.2 -29.2,-8.1 -47.7,-13.2 -61.3,-16.9 -62.4,-16.9 -0.6,0.1 -2.8,0.7 -4.9,1.5z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).build()
    }

    /**
     * Fully Solid Filled Material Symbols Artist icon (Person with music note badge).
     */
    val Artist: ImageVector by lazy {
        ImageVector.Builder(
            name = "RoundedArtist24Filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
            autoMirror = true
        ).addPath(
            pathData = PathParser().parsePathString(
                "M700,800Q658,800 629,771Q600,742 600,700Q600,658 629,629Q658,600 700,600Q708,600 718,601.5Q728,603 740,608L740,440Q740,423 751.5,411.5Q763,400 780,400L840,400Q857,400 868.5,411.5Q880,423 880,440Q880,457 868.5,468.5Q857,480 840,480L800,480L800,700Q800,742 771,771Q742,800 700,800ZM160,800Q143,800 131.5,788.5Q120,777 120,760L120,688Q120,653 137.5,625Q155,597 184,582Q246,551 310,535.5Q374,520 440,520Q475,520 508,525.5Q541,531 556,547Q571,563 565,585Q559,606 536,612Q516,615 496,616L496,720L493,720Q513,720 523,732.5Q533,745 533,760Q533,775 523,787.5Q513,800 493,800L160,800ZM440,480Q374,480 327,433Q280,386 280,320Q280,254 327,207Q374,160 440,160Q506,160 553,207Q600,254 600,320Q600,386 553,433Q506,480 440,480Z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).build()
    }

    val AlignJustifySpaceEven: ImageVector by lazy {
        ImageVector.Builder(
            name = "RoundedAlignJustifySpaceEven24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).addPath(
            pathData = PathParser().parsePathString(
                "M600,680Q583,680 571.5,668.5Q560,657 560,640L560,320Q560,303 571.5,291.5Q583,280 600,280L640,280Q657,280 668.5,291.5Q680,303 680,320L680,640Q680,657 668.5,668.5Q657,680 640,680L600,680ZM320,680Q303,680 291.5,668.5Q280,657 280,640L280,320Q280,303 291.5,291.5Q303,280 320,280L360,280Q377,280 388.5,291.5Q400,303 400,320L400,640Q400,657 388.5,668.5Q377,680 360,680L320,680ZM840,880Q823,880 811.5,868.5Q800,857 800,840L800,120Q800,103 811.5,91.5Q823,80 840,80Q857,80 868.5,91.5Q880,103 880,120L880,840Q880,857 868.5,868.5Q857,880 840,880ZM120,880Q103,880 91.5,868.5Q80,857 80,840L80,120Q80,103 91.5,91.5Q103,80 120,80Q137,80 148.5,91.5Q160,103 160,120L160,840Q160,857 148.5,868.5Q137,880 120,880Z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).build()
    }

    val GitHub: ImageVector by lazy {
        ImageVector.Builder(
            name = "GitHub",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 30f,
            viewportHeight = 30f
        ).addPath(
            pathData = PathParser().parsePathString(
                "M15,3C8.373,3 3,8.373 3,15c0,5.623 3.872,10.328 9.092,11.63C12.036,26.468 12,26.28 12,26.047v-2.051c-0.487,0 -1.303,0 -1.508,0c-0.821,0 -1.551,-0.353 -1.905,-1.009c-0.393,-0.729 -0.461,-1.844 -1.435,-2.526c-0.289,-0.227 -0.069,-0.486 0.264,-0.451c0.615,0.174 1.125,0.596 1.605,1.222c0.478,0.627 0.703,0.769 1.596,0.769c0.433,0 1.081,-0.025 1.691,-0.121c0.328,-0.833 0.895,-1.6 1.588,-1.962c-3.996,-0.411 -5.903,-2.399 -5.903,-5.098c0,-1.162 0.495,-2.286 1.336,-3.233C9.053,10.647 8.706,8.73 9.435,8c1.798,0 2.885,1.166 3.146,1.481C13.477,9.174 14.461,9 15.495,9c1.036,0 2.024,0.174 2.922,0.483C18.675,9.17 19.763,8 21.565,8c0.732,0.731 0.381,2.656 0.102,3.594c0.836,0.945 1.328,2.066 1.328,3.226c0,2.697 -1.904,4.684 -5.894,5.097C18.199,20.49 19,22.1 19,23.313v2.734c0,0.104 -0.023,0.179 -0.035,0.268C23.641,24.676 27,20.236 27,15C27,8.373 21.627,3 15,3z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).build()
    }

    val Telegram: ImageVector by lazy {
        ImageVector.Builder(
            name = "Telegram",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = PathParser().parsePathString(
                "M9.78,18.65l0.28,-4.23l7.68,-6.92c0.34,-0.31 -0.07,-0.46 -0.52,-0.19L7.74,13.3L3.64,12c-0.88,-0.25 -0.89,-0.86 0.2,-1.3l16.03,-6.19c0.73,-0.33 1.43,0.18 1.15,1.3l-2.73,12.86c-0.19,0.91 -0.74,1.13 -1.5,0.7l-4.16,-3.07l-2,1.93c-0.23,0.23 -0.42,0.42 -0.85,0.42z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).build()
    }

    val Jellyfin: ImageVector by lazy {
        ImageVector.Builder(
            name = "JellyfinLogo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = PathParser().parsePathString(
                "M12,0.002C8.826,0.002 -1.398,18.537 0.16,21.666c1.56,3.129 22.14,3.094 23.682,0C25.384,18.573 15.177,0 12,0zM19.76,18.951c-1.008,2.028 -14.493,2.05 -15.514,0C3.224,16.9 9.92,4.755 12.003,4.755c2.081,0 8.77,12.166 7.759,14.196zM12,9.198c-1.054,0 -4.446,6.15 -3.93,7.189c0.518,1.04 7.348,1.027 7.86,0c0.511,-1.027 -2.874,-7.19 -3.93,-7.19z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).build()
    }

    val Subsonic: ImageVector by lazy {
        ImageVector.Builder(
            name = "SubsonicLogo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 512f,
            viewportHeight = 512f
        ).addPath(
            pathData = PathParser().parsePathString(
                "M256,80C158.8,80 80,158.8 80,256s78.8,176 176,176s176,-78.8 176,-176S353.2,80 256,80zM256,400c-79.5,0 -144,-64.5 -144,-144s64.5,-144 144,-144s144,64.5 144,144S335.5,400 256,400z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).addPath(
            pathData = PathParser().parsePathString(
                "M256,160C202.9,160 160,202.9 160,256s42.9,96 96,96s96,-42.9 96,-96S309.1,160 256,160zM256,312c-30.9,0 -56,-25.1 -56,-56s25.1,-56 56,-56s56,25.1 56,56S286.9,312 256,312z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).addPath(
            pathData = PathParser().parsePathString(
                "M256,220c-19.9,0 -36,16.1 -36,36s16.1,36 36,36s36,-16.1 36,-36S275.9,220 256,220z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).build()
    }

    val Navidrome: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavidromeLogo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = PathParser().parsePathString(
                "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8S16.41,20 12,20z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).addPath(
            pathData = PathParser().parsePathString(
                "M12,8c-2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4S14.21,8 12,8zM12,14c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2S13.1,14 12,14z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).addPath(
            pathData = PathParser().parsePathString(
                "M12,16.5c-0.28,0 -0.5,0.22 -0.5,0.5s0.22,0.5 0.5,0.5 5,-2.24 5,-5 -0.22,-0.5 -0.5,-0.5S16,12.22 16,12.5c0,2.21 -1.79,4 -4,4z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).build()
    }
}

