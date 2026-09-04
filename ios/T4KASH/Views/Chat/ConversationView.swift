import SwiftUI

/// Conversación de un trabajo: mensajes, envío y refresco periódico.
struct ConversationView: View {
    let conversation: Conversation

    @Environment(AppDependencies.self) private var dependencies

    @State private var viewModel: ConversationViewModel?

    var body: some View {
        Group {
            if let viewModel {
                content(viewModel: viewModel)
            } else {
                LoadingStateView()
            }
        }
        .screenBackground()
        .navigationTitle(conversation.participanteVisible)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                VStack(spacing: 0) {
                    Text(conversation.participanteVisible)
                        .font(Theme.Font.headline)
                        .foregroundStyle(Theme.Color.text)
                    Text(conversation.tituloVisible)
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textMuted)
                        .lineLimit(1)
                }
            }
        }
        .onAppear {
            if viewModel == nil {
                viewModel = ConversationViewModel(
                    conversation: conversation,
                    repository: dependencies.communication
                )
            }
        }
        .task { await viewModel?.load() }
        // El sondeo se cancela solo al abandonar la pantalla.
        .task { await viewModel?.startPolling() }
    }

    @ViewBuilder
    private func content(viewModel: ConversationViewModel) -> some View {
        @Bindable var model = viewModel

        VStack(spacing: 0) {
            switch viewModel.state {
            case .idle, .loading:
                LoadingStateView()

            case let .failed(message):
                ErrorStateView(message: message) { await viewModel.load() }

            case .loaded:
                if viewModel.messages.isEmpty {
                    EmptyStateView(
                        icon: "text.bubble",
                        title: "Empieza la conversación",
                        message: "Escribe el primer mensaje para coordinar el trabajo."
                    )
                } else {
                    messageList(viewModel: viewModel)
                }
            }

            if let error = viewModel.errorMessage {
                InlineErrorBanner(message: error) { model.errorMessage = nil }
                    .padding(.horizontal, Theme.Spacing.md)
            }

            composer(viewModel: viewModel)
        }
    }

    private func messageList(viewModel: ConversationViewModel) -> some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: Theme.Spacing.xs) {
                    ForEach(Array(viewModel.messages.enumerated()), id: \.element.id) { index, message in
                        if viewModel.startsNewDay(at: index) {
                            Text(DisplayFormatter.daySeparator(message.fechaEnvio))
                                .font(Theme.Font.caption)
                                .foregroundStyle(Theme.Color.textMuted)
                                .padding(.horizontal, Theme.Spacing.sm)
                                .padding(.vertical, 4)
                                .background(Capsule().fill(Theme.Color.surfaceVariant))
                                .padding(.vertical, Theme.Spacing.xs)
                        }

                        MessageBubble(message: message)
                            .id(message.idMensaje)
                    }
                }
                .padding(Theme.Spacing.md)
            }
            .scrollDismissesKeyboard(.interactively)
            .onChange(of: viewModel.messages.count) { _, _ in
                guard let last = viewModel.messages.last else { return }
                withAnimation(.easeOut(duration: 0.2)) {
                    proxy.scrollTo(last.idMensaje, anchor: .bottom)
                }
            }
            .onAppear {
                guard let last = viewModel.messages.last else { return }
                proxy.scrollTo(last.idMensaje, anchor: .bottom)
            }
        }
    }

    private func composer(viewModel: ConversationViewModel) -> some View {
        @Bindable var model = viewModel

        return HStack(alignment: .bottom, spacing: Theme.Spacing.xs) {
            TextField("Mensaje", text: $model.draft, axis: .vertical)
                .lineLimit(1...5)
                .padding(.horizontal, Theme.Spacing.sm)
                .padding(.vertical, Theme.Spacing.xs)
                .background(
                    RoundedRectangle(cornerRadius: Theme.Radius.lg, style: .continuous)
                        .fill(Theme.Color.surfaceVariant)
                )

            AsyncButton {
                await viewModel.send()
            } label: {
                Image(systemName: "arrow.up")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .background(
                        Circle().fill(
                            viewModel.canSend ? Theme.Color.primary : Theme.Color.textSoft
                        )
                    )
            }
            .disabled(!viewModel.canSend)
            .accessibilityLabel("Enviar mensaje")
        }
        .padding(Theme.Spacing.sm)
        .background(Theme.Color.surface)
        .overlay(alignment: .top) {
            Rectangle().fill(Theme.Color.border).frame(height: 1)
        }
    }
}

/// Burbuja de mensaje. Los propios van a la derecha, como en iMessage.
struct MessageBubble: View {
    let message: Message

    var body: some View {
        HStack {
            if message.propio { Spacer(minLength: 48) }

            VStack(alignment: message.propio ? .trailing : .leading, spacing: 3) {
                if !message.propio, let emisor = message.nombreEmisor, !emisor.isEmpty {
                    Text(emisor)
                        .font(Theme.Font.caption.weight(.semibold))
                        .foregroundStyle(Theme.Color.primary)
                }

                Text(message.contenido)
                    .font(Theme.Font.body)
                    .foregroundStyle(message.propio ? .white : Theme.Color.text)
                    .multilineTextAlignment(.leading)

                HStack(spacing: 3) {
                    Text(DisplayFormatter.time(message.fechaEnvio))
                        .font(.system(size: 10))
                        .foregroundStyle(
                            message.propio ? Color.white.opacity(0.75) : Theme.Color.textSoft
                        )

                    if message.propio {
                        Image(systemName: message.leido ? "checkmark.circle.fill" : "checkmark")
                            .font(.system(size: 9))
                            .foregroundStyle(Color.white.opacity(message.leido ? 0.95 : 0.7))
                            .accessibilityLabel(message.leido ? "Leído" : "Enviado")
                    }
                }
            }
            .padding(.horizontal, Theme.Spacing.sm)
            .padding(.vertical, Theme.Spacing.xs)
            .background(
                RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                    .fill(message.propio ? Theme.Color.primary : Theme.Color.surface)
            )
            .overlay(
                RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                    .stroke(message.propio ? Color.clear : Theme.Color.border, lineWidth: 1)
            )

            if !message.propio { Spacer(minLength: 48) }
        }
    }
}
