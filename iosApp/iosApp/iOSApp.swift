import SwiftUI
import Shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                // Bottom & horizontal edge-to-edge (home indicator + video).
                // TOP respects the notch so buttons never go under the camera.
                .ignoresSafeArea(edges: [.bottom, .horizontal])
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}
