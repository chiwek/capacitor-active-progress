import ActivityKit
import WidgetKit
import SwiftUI

@available(iOS 16.1, *)
struct ActiveProgressLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: ActiveProgressAttributes.self) { context in
            VStack(alignment: .leading, spacing: 6) {
                Text(context.state.title).bold()
                ProgressView(value: max(0.0, min(context.state.progress, 1.0)))
                HStack {
                    Text(context.state.text)
                    if let eta = context.state.etaSeconds {
                        Text("ETA ~\(Int(ceil(Double(eta)/60.0))) min")
                    }
                }.font(.subheadline)
            }
            .padding(12)
            .tint(Color(uiColor: UIColor(hex: context.attributes.accentHex ?? "#FF2D55") ?? .systemPink))
            .activityBackgroundTint(Color(.secondarySystemBackground))
            .activitySystemActionForegroundColor(.primary)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) { Image(systemName: "car.fill") }
                DynamicIslandExpandedRegion(.center) {
                    VStack(alignment: .leading) {
                        Text(context.state.title).bold()
                        ProgressView(value: max(0.0, min(context.state.progress, 1.0)))
                    }
                }
                DynamicIslandExpandedRegion(.trailing) {
                    if let eta = context.state.etaSeconds { Text("~\(Int(ceil(Double(eta)/60.0)))m") }
                }
                DynamicIslandExpandedRegion(.bottom) { Text(context.state.text).font(.subheadline) }
            } compactLeading: { Image(systemName: "car.fill") }
              compactTrailing: { Text("\(Int(round(context.state.progress*100)))%") }
              minimal: { Image(systemName: "car.fill") }
            .widgetURL(URL(string: "yourapp://order/\(context.attributes.orderId)"))
            .keylineTint(Color(uiColor: UIColor(hex: context.attributes.accentHex ?? "#FF2D55") ?? .systemPink))
        }
    }
}

extension UIColor {
    convenience init?(hex: String) {
        var h = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        if h.hasPrefix("#") { h.removeFirst() }
        guard h.count == 6, let v = Int(h, radix: 16) else { return nil }
        self.init(red: CGFloat((v >> 16) & 0xFF)/255,
                  green: CGFloat((v >> 8) & 0xFF)/255,
                  blue: CGFloat(v & 0xFF)/255, alpha: 1)
    }
}
