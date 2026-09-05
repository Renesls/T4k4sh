import SwiftUI

/// Comentarios de una publicación, con respuestas anidadas.
struct PostCommentsView: View {
    let post: Post
    let onChanged: (Post) -> Void

    @Environment(AppDependencies.self) private var dependencies

    @State private var viewModel: PostCommentsViewModel?
    @State private var commentToEdit: PostComment?
    @State private var commentToDelete: PostComment?

    var body: some View {
        Group {
            if let viewModel {
                content(viewModel: viewModel)
            } else {
                LoadingStateView()
            }
        }
        .screenBackground()
        .navigationTitle("Comentarios")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            if viewModel == nil {
                viewModel = PostCommentsViewModel(post: post, repository: dependencies.feed)
            }
        }
        .task { await viewModel?.load() }
        .onDisappear {
            // Devuelve los contadores actualizados al feed.
            if let viewModel { onChanged(viewModel.post) }
        }
        .sheet(item: $commentToEdit) { comment in
            EditCommentSheet(comment: comment) { texto in
                guard let viewModel else { return false }
                return await viewModel.update(comment, contenido: texto)
            }
            .presentationDetents([.medium])
        }
        .alert(
            "¿Eliminar el comentario?",
            isPresented: Binding(
                get: { commentToDelete != nil },
                set: { if !$0 { commentToDelete = nil } }
            )
        ) {
            Button("Cancelar", role: .cancel) { commentToDelete = nil }
            Button("Eliminar", role: .destructive) {
                if let comment = commentToDelete {
                    Task { await viewModel?.delete(comment) }
                }
                commentToDelete = nil
            }
        }
    }

    @ViewBuilder
    private func content(viewModel: PostCommentsViewModel) -> some View {
        @Bindable var model = viewModel

        VStack(spacing: 0) {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: Theme.Spacing.md) {
                    originalPost(viewModel.post)

                    if let error = viewModel.actionError {
                        InlineErrorBanner(message: error) { model.actionError = nil }
                    }

                    switch viewModel.state {
                    case .idle, .loading:
                        ProgressView().tint(Theme.Color.primary).frame(maxWidth: .infinity)

                    case let .failed(message):
                        ErrorStateView(message: message) { await viewModel.load() }

                    case .loaded:
                        if viewModel.threads.isEmpty {
                            EmptyStateView(
                                icon: "bubble.left",
                                title: "Sin comentarios",
                                message: viewModel.post.permiteComentarios
                                    ? "Sé la primera persona en comentar."
                                    : "El autor desactivó los comentarios en esta publicación."
                            )
                        } else {
                            ForEach(viewModel.threads, id: \.comment.id) { thread in
                                VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                                    CommentRow(
                                        comment: thread.comment,
                                        canReply: viewModel.post.permiteComentarios,
                                        onReply: { model.replyingTo = thread.comment },
                                        onEdit: { commentToEdit = thread.comment },
                                        onDelete: { commentToDelete = thread.comment }
                                    )

                                    ForEach(thread.replies) { reply in
                                        CommentRow(
                                            comment: reply,
                                            canReply: false,
                                            onReply: {},
                                            onEdit: { commentToEdit = reply },
                                            onDelete: { commentToDelete = reply }
                                        )
                                        .padding(.leading, Theme.Spacing.xl)
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(Theme.Spacing.md)
            }
            .scrollDismissesKeyboard(.interactively)

            if viewModel.post.permiteComentarios {
                composer(viewModel: viewModel)
            } else {
                Text("Los comentarios están desactivados en esta publicación.")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textMuted)
                    .padding(Theme.Spacing.sm)
            }
        }
    }

    private func originalPost(_ post: Post) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            HStack(spacing: Theme.Spacing.xs) {
                InitialsAvatar(
                    initials: post.autor.iniciales,
                    size: 34,
                    verified: post.autor.estudianteVerificado
                )
                VStack(alignment: .leading, spacing: 1) {
                    Text(post.autor.nombreCompleto)
                        .font(Theme.Font.footnote.weight(.semibold))
                        .foregroundStyle(Theme.Color.text)
                    Text(DisplayFormatter.relative(post.fechaPublicacion))
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textSoft)
                }
                Spacer(minLength: 0)
            }

            if let contenido = post.contenido, !contenido.isEmpty {
                Text(contenido)
                    .font(Theme.Font.subheadline)
                    .foregroundStyle(Theme.Color.text)
            }
        }
        .cardSurface(padding: Theme.Spacing.sm)
    }

    private func composer(viewModel: PostCommentsViewModel) -> some View {
        @Bindable var model = viewModel

        return VStack(spacing: Theme.Spacing.xxs) {
            if let replyingTo = viewModel.replyingTo {
                HStack {
                    Text("Respondiendo a \(replyingTo.autor.nombreCompleto)")
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textMuted)
                    Spacer()
                    Button {
                        model.replyingTo = nil
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(Theme.Color.textSoft)
                    }
                    .accessibilityLabel("Cancelar respuesta")
                }
                .padding(.horizontal, Theme.Spacing.sm)
            }

            HStack(alignment: .bottom, spacing: Theme.Spacing.xs) {
                TextField("Escribe un comentario", text: $model.draft, axis: .vertical)
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
                        .frame(width: 34, height: 34)
                        .background(
                            Circle().fill(
                                viewModel.canSend ? Theme.Color.primary : Theme.Color.textSoft
                            )
                        )
                }
                .disabled(!viewModel.canSend)
                .accessibilityLabel("Enviar comentario")
            }
            .padding(Theme.Spacing.sm)
        }
        .background(Theme.Color.surface)
        .overlay(alignment: .top) {
            Rectangle().fill(Theme.Color.border).frame(height: 1)
        }
    }
}

struct CommentRow: View {
    let comment: PostComment
    let canReply: Bool
    let onReply: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.xs) {
            InitialsAvatar(
                initials: comment.autor.iniciales,
                size: 32,
                verified: comment.autor.estudianteVerificado
            )

            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 4) {
                    Text(comment.autor.nombreCompleto)
                        .font(Theme.Font.caption.weight(.semibold))
                        .foregroundStyle(Theme.Color.text)
                    Text(DisplayFormatter.relative(comment.fechaComentario))
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textSoft)
                    if comment.fechaEdicion != nil {
                        Text("· editado")
                            .font(Theme.Font.caption)
                            .foregroundStyle(Theme.Color.textSoft)
                    }
                    Spacer(minLength: 0)
                }

                Text(comment.contenido)
                    .font(Theme.Font.subheadline)
                    .foregroundStyle(Theme.Color.text)

                HStack(spacing: Theme.Spacing.md) {
                    if canReply {
                        Button("Responder", action: onReply)
                            .font(Theme.Font.caption.weight(.semibold))
                            .foregroundStyle(Theme.Color.primary)
                    }
                    if comment.propio {
                        Button("Editar", action: onEdit)
                            .font(Theme.Font.caption.weight(.semibold))
                            .foregroundStyle(Theme.Color.textMuted)
                        Button("Eliminar", action: onDelete)
                            .font(Theme.Font.caption.weight(.semibold))
                            .foregroundStyle(Theme.Color.danger)
                    }
                    Spacer(minLength: 0)
                }
            }
        }
        .padding(Theme.Spacing.sm)
        .background(
            RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                .fill(Theme.Color.surface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                .stroke(Theme.Color.border, lineWidth: 1)
        )
    }
}

/// Edición de un comentario propio.
struct EditCommentSheet: View {
    let comment: PostComment
    let onSubmit: (String) async -> Bool

    @Environment(\.dismiss) private var dismiss
    @State private var texto = ""

    private var isValid: Bool {
        let trimmed = texto.trimmingCharacters(in: .whitespacesAndNewlines)
        return !trimmed.isEmpty && trimmed.count <= 2_000
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                TextEditor(text: $texto)
                    .frame(minHeight: 140)
                    .padding(Theme.Spacing.xs)
                    .background(
                        RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                            .fill(Theme.Color.surface)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                            .stroke(Theme.Color.border, lineWidth: 1)
                    )

                Text("\(texto.count)/2000")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)

                Spacer()

                AsyncButton {
                    if await onSubmit(texto) { dismiss() }
                } label: {
                    Text("Guardar cambios")
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(!isValid)
            }
            .padding(Theme.Spacing.md)
            .screenBackground()
            .navigationTitle("Editar comentario")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
            }
            .onAppear { if texto.isEmpty { texto = comment.contenido } }
        }
    }
}
