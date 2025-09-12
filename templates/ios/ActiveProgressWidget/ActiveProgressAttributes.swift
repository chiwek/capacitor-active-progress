import ActivityKit
import Foundation

public struct ActiveProgressAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        public var progress: Double
        public var title: String
        public var text: String
        public var etaSeconds: Int?
        public var payload: [String:String]?
        public init(progress: Double, title: String, text: String, etaSeconds: Int?, payload: [String:String]?) {
            self.progress = progress
            self.title = title
            self.text = text
            self.etaSeconds = etaSeconds
            self.payload = payload
        }
    }
    public var orderId: String
    public var accentHex: String?
    public init(orderId: String, accentHex: String?) {
        self.orderId = orderId
        self.accentHex = accentHex
    }
}
