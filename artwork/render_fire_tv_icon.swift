import Foundation

guard CommandLine.arguments.count > 1 else {
    fatalError("Usage: swift artwork/render_fire_tv_icon.swift <output.png>")
}

let project = URL(fileURLWithPath: #filePath)
    .deletingLastPathComponent()
    .deletingLastPathComponent()
let generated = project.appendingPathComponent("artwork/fire_tv_icon_1280x720.png")
let output = URL(fileURLWithPath: CommandLine.arguments[1])
let renderer = project.appendingPathComponent("artwork/render_embercast_assets.swift")

let process = Process()
process.executableURL = URL(fileURLWithPath: "/usr/bin/swift")
process.arguments = [renderer.path, project.path]
try process.run()
process.waitUntilExit()
guard process.terminationStatus == 0 else {
    fatalError("EmberCast artwork generation failed")
}

if generated.standardizedFileURL != output.standardizedFileURL {
    try FileManager.default.copyItem(at: generated, to: output)
}
