import WidgetKit
import SwiftUI

@main
struct ActiveProgressWidgetBundle: WidgetBundle {
    var body: some Widget {
        ActiveProgressWidget()
        ActiveProgressLiveActivity()
    }
}
