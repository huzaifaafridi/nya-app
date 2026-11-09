import SwiftUI

/// SwiftUI's stand-in for Compose's PathParser: turns SVG path data into a Path,
/// so icons and avatar shapes keep the same strings as Kotlin.
func parsePath(_ d: String) -> Path {
    var p = PathReader(Array(d.utf8))
    return p.read()
}

private struct PathReader {
    private let s: [UInt8]
    private var i = 0
    private var out = Path()
    private var cur = CGPoint.zero
    private var start = CGPoint.zero
    private var ctrl: CGPoint? // last control point, for S and T

    init(_ s: [UInt8]) {
        self.s = s
    }

    private mutating func skip() {
        while i < s.count, s[i] == 32 || s[i] == 44 || s[i] == 10 || s[i] == 13 || s[i] == 9 { i += 1 }
    }

    private func isDigit(_ c: UInt8) -> Bool { c >= 48 && c <= 57 }

    private mutating func number() -> CGFloat {
        skip()
        let from = i
        if i < s.count, s[i] == 45 || s[i] == 43 { i += 1 }
        while i < s.count, isDigit(s[i]) { i += 1 }
        if i < s.count, s[i] == 46 {
            i += 1
            while i < s.count, isDigit(s[i]) { i += 1 }
        }
        if i < s.count, s[i] == 101 || s[i] == 69 {
            i += 1
            if i < s.count, s[i] == 45 || s[i] == 43 { i += 1 }
            while i < s.count, isDigit(s[i]) { i += 1 }
        }
        return CGFloat(Double(String(decoding: s[from..<i], as: UTF8.self)) ?? 0)
    }

    private mutating func flag() -> Bool {
        skip()
        defer { i += 1 }
        return i < s.count && s[i] == 49
    }

    private mutating func point(_ rel: Bool) -> CGPoint {
        let x = number(), y = number()
        return rel ? CGPoint(x: cur.x + x, y: cur.y + y) : CGPoint(x: x, y: y)
    }

    mutating func read() -> Path {
        var cmd: UInt8 = 0
        while true {
            skip()
            if i >= s.count { return out }
            let c = s[i]
            if (c >= 65 && c <= 90) || (c >= 97 && c <= 122) {
                cmd = c
                i += 1
            } else if cmd == 77 {
                cmd = 76 // extra pairs after M are lines
            } else if cmd == 109 {
                cmd = 108
            }
            step(cmd)
        }
    }

    private mutating func step(_ cmd: UInt8) {
        let rel = cmd >= 97
        let upper = rel ? cmd - 32 : cmd
        var next: CGPoint?
        switch upper {
        case 77: // M
            cur = point(rel)
            start = cur
            out.move(to: cur)
        case 76: // L
            cur = point(rel)
            out.addLine(to: cur)
        case 72: // H
            let x = number()
            cur.x = rel ? cur.x + x : x
            out.addLine(to: cur)
        case 86: // V
            let y = number()
            cur.y = rel ? cur.y + y : y
            out.addLine(to: cur)
        case 67: // C
            let a = point(rel), b = point(rel), e = point(rel)
            out.addCurve(to: e, control1: a, control2: b)
            cur = e
            next = b
        case 83: // S
            let a = ctrl.map { CGPoint(x: 2 * cur.x - $0.x, y: 2 * cur.y - $0.y) } ?? cur
            let b = point(rel), e = point(rel)
            out.addCurve(to: e, control1: a, control2: b)
            cur = e
            next = b
        case 81: // Q
            let a = point(rel), e = point(rel)
            out.addQuadCurve(to: e, control: a)
            cur = e
            next = a
        case 84: // T
            let a = ctrl.map { CGPoint(x: 2 * cur.x - $0.x, y: 2 * cur.y - $0.y) } ?? cur
            let e = point(rel)
            out.addQuadCurve(to: e, control: a)
            cur = e
            next = a
        case 65: // A
            let rx = abs(number()), ry = abs(number()), rot = number()
            let large = flag(), sweep = flag()
            let e = point(rel)
            arc(rx, ry, rot, large, sweep, e)
            cur = e
        case 90: // Z
            out.closeSubpath()
            cur = start
        default:
            i += 1 // unknown letter: skip it rather than loop forever
        }
        ctrl = next
    }

    // SVG arc to cubic curves, via the centre parameterisation in the SVG spec
    private mutating func arc(_ rx0: CGFloat, _ ry0: CGFloat, _ rot: CGFloat, _ large: Bool, _ sweep: Bool, _ e: CGPoint) {
        if cur == e { return }
        if rx0 == 0 || ry0 == 0 {
            out.addLine(to: e)
            return
        }
        var rx = rx0, ry = ry0
        let phi = rot * .pi / 180
        let cosP = cos(phi), sinP = sin(phi)
        let dx = (cur.x - e.x) / 2, dy = (cur.y - e.y) / 2
        let x1 = cosP * dx + sinP * dy
        let y1 = -sinP * dx + cosP * dy
        let scale = x1 * x1 / (rx * rx) + y1 * y1 / (ry * ry)
        if scale > 1 {
            rx *= scale.squareRoot()
            ry *= scale.squareRoot()
        }
        let den = rx * rx * y1 * y1 + ry * ry * x1 * x1
        let num = rx * rx * ry * ry - den
        let co = (large == sweep ? -1 : 1) * max(0, num / den).squareRoot()
        let cx1 = co * rx * y1 / ry
        let cy1 = -co * ry * x1 / rx
        let cx = cosP * cx1 - sinP * cy1 + (cur.x + e.x) / 2
        let cy = sinP * cx1 + cosP * cy1 + (cur.y + e.y) / 2
        let from = atan2((y1 - cy1) / ry, (x1 - cx1) / rx)
        var delta = atan2((-y1 - cy1) / ry, (-x1 - cx1) / rx) - from
        if sweep && delta < 0 { delta += 2 * .pi }
        if !sweep && delta > 0 { delta -= 2 * .pi }
        let count = max(1, Int(ceil(abs(delta) / (.pi / 2))))
        let step = delta / CGFloat(count)
        let t = 4 / 3 * tan(step / 4)
        func map(_ ux: CGFloat, _ uy: CGFloat) -> CGPoint {
            CGPoint(x: cx + cosP * rx * ux - sinP * ry * uy, y: cy + sinP * rx * ux + cosP * ry * uy)
        }
        for n in 0..<count {
            let a = from + CGFloat(n) * step
            let b = a + step
            out.addCurve(
                to: n == count - 1 ? e : map(cos(b), sin(b)),
                control1: map(cos(a) - t * sin(a), sin(a) + t * cos(a)),
                control2: map(cos(b) + t * sin(b), sin(b) - t * cos(b))
            )
        }
    }
}
