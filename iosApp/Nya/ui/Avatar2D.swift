import SwiftUI

enum AvatarState { case idle, listening, thinking, speaking }

// one clock (ms) drives breathing (4s), sway (6s), glances, nods, blinks and the mouth
private let PERIOD = 12000.0
private let BLINK = 160.0
private let TAU = 2 * Double.pi

private let Ink = Color(argb: 0xFF2A1A14)
private let Shade = Color(argb: 0x262A1A14)
private let Lips = Color(argb: 0xFFB0574A)
private let BandColor = Color(argb: 0xFFEFE1CD)
private let Sclera = Color(argb: 0xFFFFFBF5)
private let Spark = Color(argb: 0xFFFFE2B0)
private let Rim = Color(argb: 0x59FFD9B0)
private let Gold = Color(argb: 0xFFE8C07A)
private let Stubble = Color(argb: 0x142A1A14)

private let Line = StrokeStyle(lineWidth: 1.6, lineCap: .round)
private let Thin = StrokeStyle(lineWidth: 1, lineCap: .round)
private let Shine = StrokeStyle(lineWidth: 2.5, lineCap: .round)
private let BrowF = StrokeStyle(lineWidth: 1.2, lineCap: .round)
private let BrowM = StrokeStyle(lineWidth: 2.2, lineCap: .round)

private let GlowColors = [Color(argb: 0x40E8A86E), Color.clear]
private let IrisColors = [Color(argb: 0xFFB07A4F), Color(argb: 0xFF4A2A1A)]
private let BlushColors = [Color(argb: 0x40F08A7A), Color.clear]

private let BandPath = parsePath("M37 40c4-12 14-18 23-18s20 6 24 18c-8-6-16-8-24-8s-15 2-23 8z")
private let BodyPath = parsePath("M14 120C16 104 30 93 46 91c4 3 9 4 14 4s10-1 14-4c16 2 30 13 32 29z")
private let Star = parsePath("M0 -1Q0.2 -0.2 1 0Q0.2 0.2 0 1Q-0.2 0.2 -1 0Q-0.2 -0.2 0 -1z")
private let SparkAngles = [-2.6, -1.75, -0.7, 0.2]
private let EyeX = [49.5, 70.5]

// female: softer jaw, slim brows, lashes, earrings; male: stronger jaw, thick brows, light stubble
private let FaceF = parsePath("M60 22C74 22 84 33 84 48c0 15-11 30-24 30S36 63 36 48C36 33 46 22 60 22z")
private let FaceM = parsePath("M60 22C75 22 84 32 84 47c0 10-2 18-7 24-4 5-10 7-17 7s-13-2-17-7c-5-6-7-14-7-24 0-15 9-25 24-25z")

private let BaseHair = "M35 48C30 22 46 10 62 10c18 0 32 14 24 40-2-16-10-24-26-24S39 34 35 48z"
private let ShortHair = "M36 44C33 21 46 12 60 12s27 9 24 32c-3-12-12-16-24-16s-21 4-24 16z"
private let Bob = "M32 50C26 24 42 8 60 8s34 16 28 42c2 14 4 26 0 36-6 2-12 0-16-4H48c-4 4-10 6-16 4-4-10-2-22 0-36z"
private let Bun = "M60 0a10 10 0 1 0 .1 0z"

private func mix(_ a: UInt32, _ b: UInt32, _ t: Double, alpha: Double = 1) -> Color {
    func channel(_ shift: UInt32) -> Double {
        let x = Double((a >> shift) & 0xFF) / 255, y = Double((b >> shift) & 0xFF) / 255
        return x + (y - x) * t
    }
    return Color(.sRGB, red: channel(16), green: channel(8), blue: channel(0), opacity: alpha)
}

private let White: UInt32 = 0xFFFFFFFF
private let Black: UInt32 = 0xFF000000

/// everything that makes a companion look different; [extra] is drawn on top in the accent color
private struct Look: Sendable {
    let female: Bool
    let hair: Color
    let accent: Color
    let front: Path
    let back: Path?
    let extra: Path?
    let glasses: Bool
    let serene: Bool
    let shine: Color
    let body: [Color]

    init(
        _ female: Bool, _ hair: UInt32, _ accent: UInt32, _ front: String,
        back: String? = nil, extra: String? = nil, glasses: Bool = false, serene: Bool = false
    ) {
        self.female = female
        self.hair = Color(argb: hair)
        self.accent = Color(argb: accent)
        self.front = parsePath(front)
        self.back = back.map(parsePath)
        self.extra = extra.map(parsePath)
        self.glasses = glasses
        self.serene = serene
        shine = mix(hair, White, 0.35, alpha: 0.7)
        body = [mix(accent, White, 0.15), mix(accent, Black, 0.15)]
    }
}

private func look(_ companion: Companion, _ gender: Gender) -> Look {
    let f = gender == .female
    switch companion {
    case .wanderer:
        return Look(f, 0xFF3A2218, 0xFFB07A4F, f ? BaseHair : ShortHair,
            back: f ? Bob : nil,
            extra: "M38 30C38 14 48 6 60 6s22 8 22 24z M22 34c12-8 64-8 76 0c-10 5-66 5-76 0z")
    case .muse:
        return Look(f, 0xFF4A2A1A, 0xFFE8A86E,
            f ? "M35 50C30 22 46 8 62 8c20 0 34 16 24 44-4-18-14-26-30-24-10 2-18 12-25 22z"
              : "M35 46C31 24 44 9 62 9c16 0 28 10 26 26-5-9-14-13-26-11-11 2-20 9-27 22z",
            back: f ? "M30 50C24 20 42 6 60 6s38 12 31 44c3 24 9 40 15 54H14c6-14 12-32 16-54z" : nil)
    case .guide:
        return Look(f, 0xFF2E1E16, 0xFF8C7B6B, f ? BaseHair : ShortHair,
            back: f ? Bob : nil, glasses: true)
    case .yogi:
        return Look(f, 0xFF3A2218, 0xFFC9A26B, f ? BaseHair : ShortHair,
            back: Bun, serene: true)
    case .creator:
        return Look(f, 0xFF5A2E1E, 0xFFD9775B,
            f ? "M30 52C20 18 40 0 64 2c24 2 36 24 28 50-2-18-12-30-30-30-14 0-26 12-32 30z"
              : "M33 48C28 26 38 8 58 6c10-1 20 1 26 6 4 3 4 8 0 10 6 6 6 16 2 26-3-14-12-20-26-20-14 0-22 8-27 20z",
            back: f ? Bob : nil,
            extra: "M50 8c8 2 12 10 10 18l-5-1c1-6-1-11-7-14z")
    }
}

// three blinks per clock cycle, each gap a random 3-5s, gaps summing to PERIOD
private func blinkTimes() -> [Double] {
    let a = Int.random(in: 3000...5000)
    let b = a + Int.random(in: max(3000, 7000 - a)...(min(5000, 9000 - a)))
    return [Double(a), Double(b), 0]
}

private func eyeOpen(_ t: Double, _ blinks: [Double]) -> Double {
    for b in blinks {
        let d = (t - b + PERIOD).truncatingRemainder(dividingBy: PERIOD)
        if d < BLINK { return abs(d / BLINK * 2 - 1) }
    }
    return 1
}

// 0 -> 1 -> 0 while t is in [start, start + len)
private func bump(_ t: Double, _ start: Double, _ len: Double) -> Double {
    t < start || t >= start + len ? 0 : sin(TAU / 2 * (t - start) / len)
}

/// One frame's worth of animated values, so the drawing below stays a plain function of them.
private struct Frame {
    var t = 0.0, m = 0.0
    var listen = 0.0, think = 0.0, speak = 0.0, joy = 0.0, spark = 1.0, talk = -1.0
}

/// Canvas can't animate values itself, so the eased values advance here once per frame.
@MainActor
private final class AvatarAnim {
    let blinks = blinkTimes()
    private let began = Date()
    private var last = Date()
    private var happyAt: Date?
    private var listen = 0.0, think = 0.0, speak = 0.0, talk = 0.0

    func happy() {
        happyAt = Date()
    }

    func frame(_ now: Date, _ state: AvatarState, _ mouth: Bool?, still: Bool) -> Frame {
        let dt = min(max(now.timeIntervalSince(last), 0), 0.1)
        last = now
        let ease = still ? 1 : 1 - exp(-dt * 20)
        listen += ((state == .listening ? 1 : 0) - listen) * ease
        think += ((state == .thinking ? 1 : 0) - think) * ease
        speak += ((state == .speaking ? 1 : 0) - speak) * ease
        talk += ((mouth == true ? 1 : 0) - talk) * (1 - exp(-dt / 0.03))
        var f = Frame()
        f.t = (now.timeIntervalSince(began) * 1000).truncatingRemainder(dividingBy: PERIOD)
        f.m = still ? 0 : f.t // reduced motion: static pose, blinks still use t
        f.listen = listen
        f.think = think
        f.speak = speak
        f.talk = mouth != nil ? talk : -1
        if let happyAt {
            let ms = now.timeIntervalSince(happyAt) * 1000
            f.spark = min(1, ms / 600)
            if ms < 120 {
                f.joy = ms / 120
            } else if ms < 2000 {
                // the Kotlin spring back to rest: damping .4, stiffness 200
                let s = (ms - 120) / 1000
                f.joy = exp(-5.66 * s) * (cos(12.96 * s) + 0.437 * sin(12.96 * s))
            } else {
                self.happyAt = nil
            }
        }
        return f
    }
}

struct Avatar2D: View {
    let companion: Companion
    let skinTone: UInt32
    let gender: Gender
    var state: AvatarState = .idle
    var mouth: Bool?

    @Environment(\.accessibilityReduceMotion) private var still
    @State private var anim = AvatarAnim()

    var body: some View {
        let look = look(companion, gender)
        let skinTone = skinTone
        let blinks = anim.blinks
        TimelineView(.animation) { timeline in
            let f = anim.frame(timeline.date, state, mouth, still: still)
            Canvas { ctx, size in
                let s = min(size.width, size.height) / 120
                ctx.translateBy(x: (size.width - 120 * s) / 2, y: (size.height - 120 * s) / 2)
                ctx.scaleBy(x: s, y: s)
                drawAvatar(ctx, look, skinTone, f, blinks)
            }
        }
        .accessibilityHidden(true)
        // a simultaneous gesture, so a clickable parent still gets the tap
        .simultaneousGesture(TapGesture().onEnded { if !still { anim.happy() } })
    }
}

private func rotated(_ ctx: GraphicsContext, _ degrees: Double, _ x: Double, _ y: Double) -> GraphicsContext {
    var c = ctx
    c.translateBy(x: x, y: y)
    c.rotate(by: .degrees(degrees))
    c.translateBy(x: -x, y: -y)
    return c
}

private func scaled(_ ctx: GraphicsContext, _ sx: Double, _ sy: Double, _ x: Double, _ y: Double) -> GraphicsContext {
    var c = ctx
    c.translateBy(x: x, y: y)
    c.scaleBy(x: sx, y: sy)
    c.translateBy(x: -x, y: -y)
    return c
}

private func moved(_ ctx: GraphicsContext, _ x: Double, _ y: Double) -> GraphicsContext {
    var c = ctx
    c.translateBy(x: x, y: y)
    return c
}

private func faded(_ ctx: GraphicsContext, _ alpha: Double) -> GraphicsContext {
    var c = ctx
    c.opacity = alpha
    return c
}

private func oval(_ x: Double, _ y: Double, _ w: Double, _ h: Double) -> Path {
    Path(ellipseIn: CGRect(x: x, y: y, width: w, height: h))
}

private func circle(_ r: Double, _ x: Double, _ y: Double) -> Path {
    oval(x - r, y - r, r * 2, r * 2)
}

// Compose's drawArc: degrees clockwise from 3 o'clock around the oval in the given box
private func arc(_ start: Double, _ sweep: Double, _ x: Double, _ y: Double, _ w: Double, _ h: Double) -> Path {
    var p = Path()
    p.addRelativeArc(center: .zero, radius: 1, startAngle: .degrees(start), delta: .degrees(sweep))
    return p.applying(CGAffineTransform(translationX: x + w / 2, y: y + h / 2).scaledBy(x: w / 2, y: h / 2))
}

private func line(_ x1: Double, _ y1: Double, _ x2: Double, _ y2: Double) -> Path {
    var p = Path()
    p.move(to: CGPoint(x: x1, y: y1))
    p.addLine(to: CGPoint(x: x2, y: y2))
    return p
}

private func radial(_ colors: [Color], _ x: Double, _ y: Double, _ r: Double) -> GraphicsContext.Shading {
    .radialGradient(Gradient(colors: colors), center: CGPoint(x: x, y: y), startRadius: 0, endRadius: r)
}

private func vertical(_ colors: [Color], _ from: Double, _ to: Double) -> GraphicsContext.Shading {
    .linearGradient(Gradient(colors: colors), startPoint: CGPoint(x: 0, y: from), endPoint: CGPoint(x: 0, y: to))
}

private func drawAvatar(_ ctx: GraphicsContext, _ look: Look, _ skinTone: UInt32, _ f: Frame, _ blinks: [Double]) {
    let m = f.m
    let breath = (1 - cos(TAU * m / 4000)) / 2
    let sway = 2 * sin(TAU * m / 6000) + 4 * f.listen
    let hairSway = 1.2 * sin(TAU * (m - 500) / 6000)
    let headY = 1.5 * f.listen * bump(m.truncatingRemainder(dividingBy: 4000), 0, 500) + 0.8 * f.speak * sin(TAU * m / 400)
    let skin = vertical([mix(skinTone, White, 0.12), Color(argb: skinTone), mix(skinTone, Black, 0.1)], 22, 78)
    let skinShade = mix(skinTone, Black, 0.14)

    ctx.fill(Path(CGRect(x: 0, y: 0, width: 120, height: 120)), with: radial(GlowColors, 60, 48, 50))
    let body = scaled(moved(ctx, 0, -1.5 * breath - 6 * f.joy), 1 + 0.02 * breath, 1 + 0.02 * breath, 60, 120)
    if let back = look.back {
        rotated(body, sway + hairSway, 60, 74).fill(back, with: .color(look.hair))
    }
    body.fill(Path(CGRect(x: 53, y: 68, width: 14, height: 24)), with: .color(skinShade))
    body.fill(oval(50, 74, 20, 8), with: .color(Shade))
    body.fill(BodyPath, with: vertical(look.body, 90, 120))
    body.fill(oval(48, 106, 13, 10), with: .color(skinShade))
    body.fill(oval(59, 106, 13, 10), with: .color(skinShade))
    drawHead(moved(rotated(body, sway, 60, 74), 0, headY), look, skin, skinShade, f, blinks, hairSway)

    if f.think > 0.001 {
        for i in 0...2 {
            let d = Double(i)
            let pulse = (1 - cos(TAU * (m / 900 - d * 0.2))) / 2
            faded(ctx, f.think * (0.35 + 0.65 * pulse))
                .fill(circle(1.6 + 0.4 * d, 86 + 6 * d, 28 - 4 * d), with: .color(look.accent))
        }
    }
    if f.spark < 1 {
        for a in SparkAngles {
            let d = 26 + 16 * f.spark
            let c = moved(ctx, 60 + cos(a) * d, 44 + sin(a) * d)
            let k = 3 * (1 - f.spark) + 1
            faded(scaled(c, k, k, 0, 0), 1 - f.spark).fill(Star, with: .color(Spark))
        }
    }
}

private func drawHead(
    _ ctx: GraphicsContext, _ look: Look, _ skin: GraphicsContext.Shading, _ skinShade: Color,
    _ f: Frame, _ blinks: [Double], _ hairSway: Double
) {
    ctx.fill(oval(32, 44, 8, 14), with: .color(skinShade))
    ctx.fill(oval(80, 44, 8, 14), with: .color(skinShade))
    if look.female {
        ctx.fill(circle(1.6, 36, 58), with: .color(Gold))
        ctx.fill(circle(1.6, 84, 58), with: .color(Gold))
    }
    ctx.fill(look.female ? FaceF : FaceM, with: skin)
    ctx.stroke(arc(-70, 120, 36, 22, 48, 56), with: .color(Rim), style: Line)
    ctx.fill(circle(6, 44, 61), with: radial(BlushColors, 44, 61, 6))
    ctx.fill(circle(6, 76, 61), with: radial(BlushColors, 76, 61, 6))
    if !look.female { ctx.fill(oval(47, 64, 26, 14), with: .color(Stubble)) }

    let glance = bump(f.m, 8500, 1600) - bump(f.m, 3000, 1600)
    let gx = 1.4 * glance * (1 - f.think) + 1.3 * f.think
    let open = max(eyeOpen(f.t, blinks), 0.1)
    for cx in EyeX {
        if look.serene {
            ctx.stroke(arc(15, 150, cx - 5, 49, 10, 6), with: .color(Ink), style: Line)
        } else {
            drawEye(ctx, cx, open, 1 + 0.12 * f.listen, gx, -1.3 * f.think, look.female)
        }
    }
    let lift = 3 * f.joy
    let brow = look.female ? BrowF : BrowM
    let bh: Double = look.female ? 6 : 4
    ctx.stroke(arc(200, 120, 44, 41 - lift, 11, bh), with: .color(look.hair), style: brow)
    ctx.stroke(arc(220, 120, 65, 41 - lift - 2.5 * f.think, 11, bh), with: .color(look.hair), style: brow)
    ctx.stroke(arc(20, 140, 57, 56, 6, 4), with: .color(Shade), style: Line)

    let w = 12 + 4 * f.joy
    faded(ctx, 1 - f.speak).stroke(arc(20, 140, 60 - w / 2, 60, w, 8 + 2 * f.joy), with: .color(Lips), style: Line)
    if f.speak > 0.001 {
        // openness 0 = closed, .5 = small, 1 = open; from the voice signal, else cycling ~6 shapes a second
        let o = f.talk >= 0 ? f.talk : Double(Int(f.m / 167) % 3) / 2
        let k = o < 0.5 ? o * 2 : o * 2 - 1
        let mw = o < 0.5 ? 10 - 2 * k : 8 + 2 * k
        let mh = o < 0.5 ? 1.8 + 2.2 * k : 4 + 3 * k
        faded(ctx, f.speak).fill(oval(60 - mw / 2, 66 - mh / 2, mw, mh), with: .color(Lips))
    }
    if look.glasses {
        ctx.stroke(circle(6.5, 49.5, 52), with: .color(Ink), style: Thin)
        ctx.stroke(circle(6.5, 70.5, 52), with: .color(Ink), style: Thin)
        ctx.stroke(line(56, 52, 64, 52), with: .color(Ink), lineWidth: 1)
    }

    let hair = rotated(ctx, hairSway, 60, 30)
    moved(hair, 0, 1.5).fill(look.front, with: .color(Shade))
    hair.fill(look.front, with: .color(look.hair))
    hair.stroke(arc(205, 50, 44, 14, 28, 28), with: .color(look.shine), style: Shine)
    ctx.fill(BandPath, with: .color(BandColor))
    if let extra = look.extra { hair.fill(extra, with: .color(look.accent)) }
}

// sclera, iris with gradient, pupil and highlight, optional lashes; squashed vertically to blink
private func drawEye(_ ctx: GraphicsContext, _ cx: Double, _ open: Double, _ widen: Double, _ gx: Double, _ gy: Double, _ lashes: Bool) {
    let eye = scaled(ctx, widen, widen * open, cx, 52)
    eye.fill(oval(cx - 5, 46.5, 10, 11), with: .color(Sclera))
    let iris = moved(eye, cx + gx, 52 + gy)
    iris.fill(circle(3.4, 0, 0), with: radial(IrisColors, 0, 0, 3.4))
    iris.fill(circle(1.6, 0, 0), with: .color(Ink))
    iris.fill(circle(0.9, -1, -1.2), with: .color(.white))
    eye.stroke(arc(200, 140, cx - 5, 46.5, 10, 11), with: .color(Ink), style: Thin)
    if lashes {
        let d: Double = cx < 60 ? -1 : 1 // toward the outer corner
        eye.stroke(line(cx + 4 * d, 48.5, cx + 6.5 * d, 46), with: .color(Ink), style: Thin)
        eye.stroke(line(cx + 2.5 * d, 47.2, cx + 4 * d, 44.6), with: .color(Ink), style: Thin)
    }
}
