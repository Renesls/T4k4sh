import SwiftUI

/// Feed universitario con los tres alcances del backend.
struct FeedView: View {
    @Environment(AppDependencies.self) private var dependencies

    @State private var viewModel: FeedViewModel?
    @State private var showComposer = false
    @State private var postToEdit: Post?
    @State private var postToDelete: Post?

    var body: some View {
        Group {
            if let viewModel {
                content(viewModel: viewModel)
            } else {
                LoadingStateView()
            }
        }
        .screenBackground()
        .navigationTitle(viewModel?.showingSaved == true ? "Guardadas" : "Red")
        .navigationBarTitleDisplayMode(.large)
        .toolbar { toolbarContent }
        .onAppear {
            if viewModel == nil {
                viewModel = FeedViewModel(repository: dependencies.feed)
            }
        }
        .task { await viewModel?.load() }
        .sheet(isPresented: $showComposer) {
            if let viewModel {
                PostComposerSheet(mode: .create) { contenido, tipo, visibilidad, comentarios in
                    await viewModel.createPost(
                        contenido: contenido,
                        tipo: tipo,
                        visibilidad: visibilidad,
                        permiteComentarios: comentarios
                    )
                }
            }
        }
        .sheet(item: $postToEdit) { post in
            if let viewModel {
                PostComposerSheet(mode: .edit(post)) { contenido, tipo, visibilidad, comentarios in
                    await viewModel.updatePost(
                        post,
                        contenido: contenido,
                        tipo: tipo,
                        visibilidad: visibilidad,
                        permiteComentarios: comentarios
                    )
                }
            }
        }
        .alert(
            "¿Eliminar la publicación?",
            isPresented: Binding(
                get: { postToDelete != nil },
                set: { if !$0 { postToDelete = nil } }
            )
        ) {
            Button("Cancelar", role: .cancel) { postToDelete = nil }
            Button("Eliminar", role: .destructive) {
                if let post = postToDelete {
                    Task { await viewModel?.delete(post) }
                }
                postToDelete = nil
            }
        } message: {
            Text("Dejará de verse en el feed y no se puede deshacer.")
        }
    }

    @ViewBuilder
    private func content(viewModel: FeedViewModel) -> some View {
        VStack(spacing: 0) {
            if !viewModel.showingSaved {
                scopePicker(viewModel: viewModel)
            }

            switch viewModel.state {
            case .idle, .loading:
                LoadingStateView()

            case let .failed(message):
                ErrorStateView(message: message) { await viewModel.load() }

            case let .loaded(posts):
                if posts.isEmpty {
                    EmptyStateView(
                        icon: viewModel.showingSaved ? "bookmark" : "text.bubble",
                        title: viewModel.showingSaved
                            ? "No has guardado publicaciones"
                            : "Todavía no hay publicaciones aquí",
                        message: viewModel.showingSaved
                            ? "Toca el marcador en cualquier publicación para guardarla y leerla después."
                            : "Comparte un proyecto, un logro o una pregunta para empezar la conversación.",
                        actionTitle: viewModel.showingSaved ? "Volver al feed" : "Publicar",
                        action: {
                            if viewModel.showingSaved {
                                Task { await viewModel.toggleSavedView() }
                            } else {
                                showComposer = true
                            }
                        }
                    )
                } else {
                    ScrollView {
                        LazyVStack(spacing: Theme.Spacing.md) {
                            if let error = viewModel.actionError {
                                InlineErrorBanner(message: error) { viewModel.actionError = nil }
                            }

                            ForEach(posts) { post in
                                PostCard(
                                    post: post,
                                    onReact: { reaccion in
                                        await viewModel.toggleReaction(post, reaccion: reaccion)
                                    },
                                    onToggleSaved: { await viewModel.toggleSaved(post) },
                                    onEdit: { postToEdit = post },
                                    onDelete: { postToDelete = post },
                                    onCommentsChanged: { updated in viewModel.replace(updated) }
                                )
                                .task { await viewModel.loadMoreIfNeeded(currentItem: post) }
                            }

                            if viewModel.isLoadingMore {
                                ProgressView()
                                    .tint(Theme.Color.primary)
                                    .padding(.vertical, Theme.Spacing.md)
                            }
                        }
                        .padding(Theme.Spacing.md)
                    }
                    .refreshable { await viewModel.load() }
                }
            }
        }
    }

    private func scopePicker(viewModel: FeedViewModel) -> some View {
        Picker("Alcance", selection: Binding(
            get: { viewModel.scope },
            set: { newValue in Task { await viewModel.switchTo(scope: newValue) } }
        )) {
            ForEach(Domain.FeedScope.allCases) { scope in
                Text(scope.label).tag(scope)
            }
        }
        .pickerStyle(.segmented)
        .padding(.horizontal, Theme.Spacing.md)
        .padding(.bottom, Theme.Spacing.xs)
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItemGroup(placement: .topBarTrailing) {
            Button {
                Task { await viewModel?.toggleSavedView() }
            } label: {
                Image(systemName: viewModel?.showingSaved == true ? "bookmark.fill" : "bookmark")
            }
            .accessibilityLabel("Publicaciones guardadas")

            Button {
                showComposer = true
            } label: {
                Image(systemName: "square.and.pencil")
            }
            .accessibilityLabel("Nueva publicación")
        }
    }
}

/// Tarjeta de publicación con reacciones, guardado y acceso a comentarios.
struct PostCard: View {
    let post: Post
    let onReact: (Domain.Reaccion) async -> Void
    let onToggleSaved: () async -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void
    let onCommentsChanged: (Post) -> Void

    @State private var showReactionPicker = false

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            header

            if let contenido = post.contenido, !contenido.isEmpty {
                Text(contenido)
                    .font(Theme.Font.body)
                    .foregroundStyle(Theme.Color.text)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            if post.totalReacciones > 0 || post.totalComentarios > 0 {
                summaryRow
            }

            Divider().overlay(Theme.Color.border)

            actionsRow
        }
        .cardSurface()
        .confirmationDialog("Reaccionar", isPresented: $showReactionPicker, titleVisibility: .visible) {
            ForEach(Domain.Reaccion.allCases) { reaccion in
                Button("\(reaccion.emoji)  \(reaccion.label)") {
                    Task { await onReact(reaccion) }
                }
            }
            if post.reaccionPropia != nil {
                Button("Quitar mi reacción", role: .destructive) {
                    if let actual = post.reaccionPropia {
                        Task { await onReact(actual) }
                    }
                }
            }
        }
    }

    private var header: some View {
        HStack(spacing: Theme.Spacing.sm) {
            NavigationLink {
                PublicProfileView(username: post.autor.nombreUsuario ?? "")
            } label: {
                InitialsAvatar(
                    initials: post.autor.iniciales,
                    size: 42,
                    verified: post.autor.estudianteVerificado
                )
            }
            .buttonStyle(.plain)
            .disabled((post.autor.nombreUsuario ?? "").isEmpty)

            VStack(alignment: .leading, spacing: 2) {
                Text(post.autor.nombreCompleto)
                    .font(Theme.Font.headline)
                    .foregroundStyle(Theme.Color.text)
                    .lineLimit(1)

                HStack(spacing: 4) {
                    if !post.autor.arroba.isEmpty {
                        Text(post.autor.arroba)
                            .foregroundStyle(Theme.Color.primary)
                    }
                    Text("·")
                    Text(DisplayFormatter.relative(post.fechaPublicacion))
                    if post.editada {
                        Text("· editada")
                    }
                }
                .font(Theme.Font.caption)
                .foregroundStyle(Theme.Color.textSoft)
            }

            Spacer(minLength: 0)

            VStack(alignment: .trailing, spacing: 4) {
                if let visibilidad = post.visibilidadResuelta {
                    Image(systemName: visibilidad.icon)
                        .font(.caption)
                        .foregroundStyle(Theme.Color.textSoft)
                        .accessibilityLabel("Visibilidad: \(visibilidad.label)")
                }

                if post.propia {
                    Menu {
                        Button { onEdit() } label: {
                            Label("Editar", systemImage: "pencil")
                        }
                        Button(role: .destructive) { onDelete() } label: {
                            Label("Eliminar", systemImage: "trash")
                        }
                    } label: {
                        Image(systemName: "ellipsis")
                            .foregroundStyle(Theme.Color.textSoft)
                    }
                    .accessibilityLabel("Opciones de la publicación")
                }
            }
        }
    }

    private var summaryRow: some View {
        HStack(spacing: Theme.Spacing.xs) {
            if post.totalReacciones > 0 {
                HStack(spacing: -4) {
                    ForEach(post.reaccionesOrdenadas.prefix(3), id: \.reaccion) { item in
                        Text(item.reaccion.emoji)
                            .font(.caption)
                            .padding(3)
                            .background(Circle().fill(Theme.Color.surfaceVariant))
                    }
                }
                Text("\(post.totalReacciones)")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textMuted)
            }

            Spacer(minLength: 0)

            if post.totalComentarios > 0 {
                Text("\(post.totalComentarios) comentario(s)")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textMuted)
            }
        }
    }

    private var actionsRow: some View {
        HStack(spacing: 0) {
            Button {
                showReactionPicker = true
            } label: {
                Label(
                    post.reaccionPropia?.label ?? "Reaccionar",
                    systemImage: post.reaccionPropia == nil ? "hand.thumbsup" : "hand.thumbsup.fill"
                )
                .font(Theme.Font.footnote.weight(.medium))
                .foregroundStyle(post.reaccionPropia == nil ? Theme.Color.textMuted : Theme.Color.primary)
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.plain)

            NavigationLink {
                PostCommentsView(post: post, onChanged: onCommentsChanged)
            } label: {
                Label(
                    post.permiteComentarios ? "Comentar" : "Ver comentarios",
                    systemImage: "bubble.right"
                )
                .font(Theme.Font.footnote.weight(.medium))
                .foregroundStyle(Theme.Color.textMuted)
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.plain)

            AsyncButton(action: onToggleSaved) {
                Label(
                    post.guardada ? "Guardada" : "Guardar",
                    systemImage: post.guardada ? "bookmark.fill" : "bookmark"
                )
                .font(Theme.Font.footnote.weight(.medium))
                .foregroundStyle(post.guardada ? Theme.Color.primary : Theme.Color.textMuted)
                .frame(maxWidth: .infinity)
            }
        }
    }
}
