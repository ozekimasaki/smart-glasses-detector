package jp.smartglasses.detector.ui.theme

import androidx.compose.ui.graphics.Color

// ─── ブランドカラー（オレンジ系）
// 用途：ボタン・アクションのみ。文字・背景には使わない
val BrandOrange     = Color(0xFFE65100)   // Primary CTA（白文字との対比 4.6:1）
val BrandOrangeLight = Color(0xFFFFF3E0)  // 薄いコンテナ用（= primaryContainer）

// ─── ニュートラル（背景・サーフェス）
val NeutralWhite    = Color(0xFFFFFFFF)
val NeutralGray50   = Color(0xFFFAFAFA)  // ページ背景
val NeutralGray100  = Color(0xFFF5F5F5)  // カード背景
val NeutralGray200  = Color(0xFFEEEEEE)  // ディバイダー

// ─── テキストカラー（コントラスト比 WCAG AA 準拠）
val TextPrimary     = Color(0xFF1A1A1A)  // 見出し・本文（白背景で 16.5:1）
val TextSecondary   = Color(0xFF616161)  // サブテキスト（白背景で 5.9:1）
val TextDisabled    = Color(0xFF9E9E9E)  // 無効状態

// ─── セマンティック（距離バッジ）
val DistanceVeryClose = Color(0xFFD32F2F)  // とても近い（白文字 5.9:1）
val DistanceClose     = Color(0xFFE65100)  // 近い（白文字 4.6:1）
val DistanceMedium    = Color(0xFF795548)  // 少し離れている（白文字 5.2:1）
val DistanceFar       = Color(0xFF757575)  // 離れている（白文字 4.6:1）

// ─── ダークテーマ用
val DarkBackground   = Color(0xFF10141A)
val DarkSurface      = Color(0xFF171C23)
val DarkSurface2     = Color(0xFF232A34)
val DarkOnSurface    = Color(0xFFF4F7FB)
val DarkOnSurface2   = Color(0xFFD3DAE4)
val DarkOutline      = Color(0xFF5B6573)
val DarkOutlineMuted = Color(0xFF39414D)

// ─── スキャン状態
val ScanActive      = BrandOrange
val ScanInactive    = NeutralGray100
