import SwiftUI

/// Notificaciones internas.
///
/// El backend no tiene registro de dispositivos ni envío push (ni APNs ni FCM),
/// así que este es el canal real de avisos: se consultan y se marcan leídas
/// contra `/api/notifications`.
struct NotificationsView: View {
    @Binding var unreadCount: Int

    @Environment(AppDependencies.self) private var dependencies

    @State private var state: LoadState<[AppNotification]> = .idle
    @State private var actionError: String?

    private var unread: Int {
        (state.value ?? []).filter { !$0.leida }.count
    }

    var body: some View {
        Group {
            switch state {
            case .idle, .loading:
                LoadingStateView()

            case let .failed(message):
                ErrorStateView(message: message) { await load() }

            case let .loaded(notifications):
                if notifications.isEmpty {
                    EmptyStateView(
                        icon: "bell.slash",
                        title: "Sin notificaciones",
                        message: "Te avisaremos cuando recibas postulaciones, entregas o cambios en tus pagos."
                    )
                } else {
                    ScrollView {
                        LazyVStack(spacing: Theme.Spacing.xs) {
                            if let actionError {
                                InlineErrorBanner(message: actionError) { self.actionError = nil }
                            }

                            ForEach(notifications) { notification in
                                NotificationRow(notification: notification) {
                                    await markRead(notification)
                                }
                            }
                        }
                        .padding(Theme.Spacing.md)
                    }
                    .refreshable { await load() }
                }
            }
        }
        .screenBackground()
        .navigationTitle("Notificaciones")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                AsyncButton { await markAllRead() } label: {
                    Text("Marcar todas")
                }
                .disabled(unread == 0)
            }
        }
        .task { await load() }
    }

    private func load() async {
        if state.value == nil { state = .loading }
        do {
            let notifications = try await dependencies.communication.notifications()
            state = .loaded(
                notifications.sorted {
                    ($0.fechaCreacion ?? .distantPast) > ($1.fechaCreacion ?? .distantPast)
                }
            )
            unreadCount = unread
        } catch {
            state = .failed(ErrorPresenter.message(for: error))
        }
    }

    private func markRead(_ notification: AppNotification) async {
        guard !notification.leida else { return }
        do {
            let updated = try await dependencies.communication.markNotificationRead(
                id: notification.idNotificacion
            )
            guard case let .loaded(current) = state,
                  let index = current.firstIndex(where: { $0.id == updated.id })
            else { return }

            var list = current
            list[index] = updated
            state = .loaded(list)
            unreadCount = list.filter { !$0.leida }.count
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    private func markAllRead() async {
        do {
            try await dependencies.communication.markAllNotificationsRead()
            await load()
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }
}

struct NotificationRow: View {
    let notification: AppNotification
    let onRead: () async -> Void

    var body: some View {
        AsyncButton(action: onRead) {
            HStack(alignment: .top, spacing: Theme.Spacing.sm) {
                Circle()
                    .fill(notification.leida ? Theme.Color.border : Theme.Color.primary)
                    .frame(width: 8, height: 8)
                    .padding(.top, 6)

                VStack(alignment: .leading, spacing: 3) {
                    Text(notification.titulo)
                        .font(Theme.Font.headline)
                        .foregroundStyle(Theme.Color.text)
                        .multilineTextAlignment(.leading)

                    if let mensaje = notification.mensaje, !mensaje.isEmpty {
                        Text(mensaje)
                            .font(Theme.Font.subheadline)
                            .foregroundStyle(Theme.Color.textMuted)
                            .multilineTextAlignment(.leading)
                    }

                    Text(DisplayFormatter.relative(notification.fechaCreacion))
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textSoft)
                }

                Spacer(minLength: 0)
            }
            .cardSurface(padding: Theme.Spacing.sm)
        }
        .buttonStyle(.plain)
        .disabled(notification.leida)
    }
}
