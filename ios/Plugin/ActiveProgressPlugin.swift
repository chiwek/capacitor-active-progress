import Foundation
import Capacitor
import ActivityKit

@objc(ActiveProgressPlugin)
public class ActiveProgressPlugin: CAPPlugin {

    // orderId -> Activity
    private var activities: [String: Any] = [:]  // store as Any to avoid generic exposure

    @objc public override func load() {
        // nothing atm
    }

    @objc func start(_ call: CAPPluginCall) {
        guard #available(iOS 16.1, *) else {
            call.reject("Live Activities require iOS 16.1+")
            return
        }
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            call.reject("Live Activities not enabled by user")
            return
        }
        guard let orderId = call.getString("orderId"), !orderId.isEmpty else {
            call.reject("orderId required")
            return
        }

        let title = call.getString("title") ?? "Driver on the way"
        let text = call.getString("text") ?? ""
        let progress = min(max(call.getInt("progress") ?? 0, 0), 100)
        let etaSeconds = call.getInt("etaSeconds")
        let accentHex = call.getString("accentColor")
        let payload = (call.getObject("payload") as? [String: String]) ?? [:]

        if #available(iOS 16.1, *) {
            let attributes = ActiveProgressAttributes(orderId: orderId, accentHex: accentHex)
            let content = ActiveProgressAttributes.ContentState(
                progress: Double(progress) / 100.0,
                title: title,
                text: text,
                etaSeconds: etaSeconds,
                payload: payload
            )
            do {
                let activity = try Activity<ActiveProgressAttributes>.request(
                    attributes: attributes,
                    contentState: content,
                    pushType: .token // we want push updates
                )
                activities[orderId] = activity
                call.resolve()
            } catch {
                call.reject("Failed to start activity: \(error.localizedDescription)")
            }
        }
    }

    @objc func getIosActivityPushToken(_ call: CAPPluginCall) {
        guard #available(iOS 16.1, *) else {
            call.reject("Live Activities require iOS 16.1+")
            return
        }
        guard let orderId = call.getString("orderId"),
              let anyActivity = activities[orderId],
              let activity = anyActivity as? Activity<ActiveProgressAttributes> else {
            call.reject("No activity for orderId")
            return
        }

        Task {
            for await data in activity.pushTokenUpdates {
                let token = data.base64EncodedString()
                call.resolve([
                    "orderId": orderId,
                    "activityToken": token
                ])
                break
            }
        }
    }

    @objc func update(_ call: CAPPluginCall) {
        guard #available(iOS 16.1, *) else { call.resolve(); return }
        guard let orderId = call.getString("orderId"),
              let anyActivity = activities[orderId],
              let activity = anyActivity as? Activity<ActiveProgressAttributes> else {
            call.resolve(); return
        }

        let newTitle = call.getString("title")
        let newText = call.getString("text")
        let p = call.getInt("progress")
        let eta = call.getInt("etaSeconds")
        let payload = (call.getObject("payload") as? [String: String])

        let cur = activity.contentState
        let newState = ActiveProgressAttributes.ContentState(
            progress: p != nil ? Double(min(max(p!, 0), 100)) / 100.0 : cur.progress,
            title: newTitle ?? cur.title,
            text: newText ?? cur.text,
            etaSeconds: eta ?? cur.etaSeconds,
            payload: payload ?? cur.payload
        )

        Task { await activity.update(using: newState) }
        call.resolve()
    }

    @objc func stop(_ call: CAPPluginCall) {
        guard #available(iOS 16.1, *) else { call.resolve(); return }
        guard let orderId = call.getString("orderId"),
              let anyActivity = activities[orderId],
              let activity = anyActivity as? Activity<ActiveProgressAttributes> else {
            call.resolve(); return
        }

        Task {
            await activity.end(dismissalPolicy: .immediate)
            self.activities.removeValue(forKey: orderId)
            call.resolve()
        }
    }
}
