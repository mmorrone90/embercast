import AppKit
import CoreGraphics

let width = 1280
let height = 720
let bitmap = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: width, pixelsHigh: height, bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
let graphics = NSGraphicsContext(bitmapImageRep: bitmap)!
NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = graphics
let ctx = graphics.cgContext
ctx.setFillColor(NSColor(calibratedRed: 0.105, green: 0.165, blue: 0.153, alpha: 1).cgColor)
ctx.fill(CGRect(x: 0, y: 0, width: width, height: height))
let mint = NSColor(calibratedRed: 0.412, green: 0.851, blue: 0.796, alpha: 1)
ctx.saveGState()
ctx.translateBy(x: 620, y: 480)
ctx.scaleBy(x: 1.8, y: 1.8)
ctx.translateBy(x: -620, y: -380)
ctx.setStrokeColor(mint.cgColor)
ctx.setFillColor(mint.cgColor)
ctx.setLineWidth(20)
ctx.setLineCap(.round)
ctx.setLineJoin(.round)
let screen = CGMutablePath()
screen.move(to: CGPoint(x: 528, y: 470))
screen.addLine(to: CGPoint(x: 705, y: 470))
screen.addLine(to: CGPoint(x: 735, y: 440))
screen.addLine(to: CGPoint(x: 735, y: 328))
screen.addLine(to: CGPoint(x: 705, y: 298))
screen.addLine(to: CGPoint(x: 656, y: 298))
ctx.addPath(screen)
ctx.strokePath()
let waveOuter = CGMutablePath()
waveOuter.addArc(center: CGPoint(x: 518, y: 288), radius: 116, startAngle: 0, endAngle: .pi / 2, clockwise: false)
ctx.addPath(waveOuter)
ctx.strokePath()
let waveInner = CGMutablePath()
waveInner.addArc(center: CGPoint(x: 518, y: 288), radius: 55, startAngle: 0, endAngle: .pi / 2, clockwise: false)
ctx.addPath(waveInner)
ctx.strokePath()
ctx.fillEllipse(in: CGRect(x: 507, y: 277, width: 22, height: 22))
ctx.restoreGState()
let title = "FIREMIRROR" as NSString
let font = NSFont.systemFont(ofSize: 112, weight: .bold)
let attributes: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: mint, .kern: 4]
let size = title.size(withAttributes: attributes)
title.draw(at: CGPoint(x: CGFloat(width) / 2 - size.width / 2, y: 70), withAttributes: attributes)
NSGraphicsContext.restoreGraphicsState()
let data = bitmap.representation(using: .png, properties: [:])!
try data.write(to: URL(fileURLWithPath: CommandLine.arguments[1]))
