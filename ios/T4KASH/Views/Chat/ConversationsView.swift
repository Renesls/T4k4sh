import SwiftUI

/// Listado de conversaciones vinculadas a tareas y trabajos.
struct ConversationsView: View {
    @Environment(AppDependencies.self) private var dependencies

    @State private var viewModel: ConversationsViewModel?

    var body: some View {
        Group {
            if let viewModel {
                content(viewModel: viewModel)
            } else {
                LoadingStateView()
            }
        }
        .screenBackground()
        .navigationTitle("Conversaciones")
        .navigationBarTitleDisplayMode(.large)
        .onAppear {
            if viewModel == nil {
                viewModel = ConversationsViewModel(repository: dependencies.communication)
            }
        }
        .task { await viewModel?.load() }
    }

    @ViewBuilder
    private func content(viewModel: ConversationsViewModel) -> some View {
        switch viewModel.state {
        case .idle, .loading:
            LoadingStateView()

        case let .failed(message):
            ErrorStateView(message: message) { await viewModel.load() }

        case let .loaded(conversations):
            if conversations.isEmpty {
                EmptyStateView(
                    icon: "bubble.left.and.bubble.right",
                    title: "Todavía no tienes conversaciones",
                    message: "Se crean automáticamente cuando se asigna un trabajo, "
                        + "para que cliente y estudiante puedan coordinar."
                )
            } else {
                ScrollView {
                    LazyVStack(spacing: Theme.Spacing.xs) {
                        ForEach(conversations) { conversation in
                            NavigationLink {
                                ConversationView(conversation: conversation)
                            } label: {
                                ConversationRow(conversation: conversation)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(Theme.Spacing.md)
                }
                .refreshable { await viewModel.load() }
            }
        }
    }
}

struct ConversationRow: View {
    let conversation: Conversation

    var body: some View {
        HStack(spacing: Theme.Spacing.sm) {
            InitialsAvatar(initials: conversation.iniciales, size: 46)

            VStack(alignment: .leading, spacing: 3) {
                HStack {
                    Text(conversation.participanteVisible)
                        .font(Theme.Font.headline)
                        .foregroundStyle(Theme.Color.text)
                        .lineLimit(1)
                    Spacer(minLength: Theme.Spacing.xs)
                    Text(DisplayFormatter.relative(conversation.fechaUltimoMensaje))
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textSoft)
                }

                Text(conversation.tituloVisible)
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.primary)
                    .lineLimit(1)

                HStack {
                    Text(conversation.ultimoMensaje ?? "Sin mensajes todavía")
                        .font(Theme.Font.subheadline)
                        .foregroundStyle(Theme.Color.textMuted)
                        .lineLimit(1)

                    Spacer(minLength: Theme.Spacing.xs)

                    if conversation.mensajesNoLeidos > 0 {
                        Text("\(conversation.mensajesNoLeidos)")
                            .font(Theme.Font.caption.weight(.bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 3)
                            .background(Capsule().fill(Theme.Color.primary))
                    }
                }
            }
        }
        .cardSurface(padding: Theme.Spacing.sm)
    }
}
