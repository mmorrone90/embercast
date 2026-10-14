import AppKit
import CoreGraphics

let project = CommandLine.arguments[1]
func color(_ h: UInt32) -> NSColor {
    NSColor(srgbRed: CGFloat((h >> 16) & 255) / 255, green: CGFloat((h >> 8) & 255) / 255, blue: CGFloat(h & 255) / 255, alpha: 1)
}
let bg = color(0xFFB184), ink = color(0x1B1917)

func draw(_ path: String, width: Int, height: Int, banner: Bool = false, store: Bool = false) throws {
    let bmp = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: width, pixelsHigh: height, bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: bmp)
    let ctx = NSGraphicsContext.current!.cgContext
    ctx.setFillColor(bg.cgColor)
    ctx.fill(CGRect(x: 0, y: 0, width: width, height: height))
    let scale = banner ? CGFloat(width) / 320 * 0.76 : store ? 2.4 : CGFloat(width) / 320 * 1.25
    let cx = CGFloat(width) / 2
    let top = banner ? 10 * scale : store ? 138 : 38 * scale
    let ox = cx - 48 * scale
    let oy = CGFloat(height) - top - 88 * scale
    ctx.setStrokeColor(ink.cgColor)
    ctx.setFillColor(bg.cgColor)
    ctx.setLineWidth(5.5 * scale)
    ctx.setLineJoin(.round)
    let tv = CGRect(x: ox + 12 * scale, y: oy + 22 * scale, width: 76 * scale, height: 48 * scale)
    ctx.addPath(CGPath(roundedRect: tv, cornerWidth: 6.5 * scale, cornerHeight: 6.5 * scale, transform: nil))
    ctx.drawPath(using: .stroke)
    let phone = CGRect(x: ox + 8 * scale, y: oy + 3 * scale, width: 31 * scale, height: 42 * scale)
    ctx.addPath(CGPath(roundedRect: phone, cornerWidth: 5 * scale, cornerHeight: 5 * scale, transform: nil))
    ctx.drawPath(using: .stroke)
    ctx.setFillColor(ink.cgColor)
    let line = CGRect(x: ox + 19 * scale, y: oy + 9.9 * scale, width: 9 * scale, height: 2.2 * scale)
    ctx.addPath(CGPath(roundedRect: line, cornerWidth: 1.1 * scale, cornerHeight: 1.1 * scale, transform: nil))
    ctx.fillPath()
    ctx.fillEllipse(in: CGRect(x: ox + 57 * scale, y: oy + 16 * scale, width: 5.5 * scale, height: 5.5 * scale))
    if banner || store {
        let fontSize = banner ? 28 : 108
        let font = NSFont.systemFont(ofSize: CGFloat(fontSize), weight: .bold)
        let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: ink, .kern: banner ? -0.4 : -2.0]
        let title = "EmberCast" as NSString
        let measured = title.size(withAttributes: attrs)
        let y = banner ? 12.0 : 102.0
        title.draw(at: CGPoint(x: CGFloat(width) / 2 - measured.width / 2, y: y), withAttributes: attrs)
    }
    NSGraphicsContext.restoreGraphicsState()
    try bmp.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: path))
}
try draw(project + "/receiver/app/src/main/res/drawable/embercast_square_icon.png", width: 320, height: 320)
try draw(project + "/receiver/app/src/main/res/drawable/embercast_banner.png", width: 320, height: 180, banner: true)
try draw(project + "/artwork/embercast_store_artwork_1280x720.png", width: 1280, height: 720, store: true)
