import PhotosUI
import SwiftUI

/// Lista de adjuntos con descarga y vista previa.
struct AttachmentList: View {
    let attachments: [Attachment]
    let repository: AttachmentRepository

    @State private var downloadingId: Int?
    @State private var shareURL: URL?
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: Theme.Spacing.xs) {
            if let errorMessage {
                InlineErrorBanner(message: errorMessage) { self.errorMessage = nil }
            }

            ForEach(attachments) { attachment in
                Button {
                    Task { await download(attachment) }
                } label: {
                    HStack(spacing: Theme.Spacing.sm) {
                        Image(systemName: attachment.iconoSistema)
                            .font(.title3)
                            .foregroundStyle(Theme.Color.primary)
                            .frame(width: 32)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(attachment.nombreOriginal)
                                .font(Theme.Font.footnote.weight(.medium))
                                .foregroundStyle(Theme.Color.text)
                                .lineLimit(1)
                            Text(DisplayFormatter.fileSize(attachment.tamanoBytes))
                                .font(Theme.Font.caption)
                                .foregroundStyle(Theme.Color.textMuted)
                        }

                        Spacer(minLength: 0)

                        if downloadingId == attachment.idArchivo {
                            ProgressView().controlSize(.small)
                        } else {
                            Image(systemName: "arrow.down.circle")
                                .foregroundStyle(Theme.Color.textSoft)
                        }
                    }
                    .padding(Theme.Spacing.sm)
                    .background(
                        RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                            .fill(Theme.Color.surfaceVariant)
                    )
                }
                .buttonStyle(.plain)
                .disabled(downloadingId != nil)
            }
        }
        .sheet(item: Binding(
            get: { shareURL.map(ShareableFile.init) },
            set: { shareURL = $0?.url }
        )) { file in
            ShareSheet(items: [file.url])
        }
    }

    private func download(_ attachment: Attachment) async {
        downloadingId = attachment.idArchivo
        defer { downloadingId = nil }
        do {
            shareURL = try await repository.download(attachment)
        } catch {
            errorMessage = ErrorPresenter.message(for: error)
        }
    }
}

/// Envoltorio para presentar un archivo con `.sheet(item:)`.
struct ShareableFile: Identifiable {
    let id = UUID()
    let url: URL
}

/// Menú para adjuntar desde fototeca, cámara o archivos.
///
/// Cubre las tres fuentes que ofrece Android, con las API nativas de Apple:
/// `PhotosPicker`, `UIImagePickerController` y `fileImporter`.
struct AttachmentPickerMenu: View {
    let title: String
    let onPrepared: (PreparedAttachment) async -> Void

    @State private var photoItem: PhotosPickerItem?
    @State private var showCamera = false
    @State private var showFileImporter = false
    @State private var isPreparing = false
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: Theme.Spacing.xs) {
            if let errorMessage {
                InlineErrorBanner(message: errorMessage) { self.errorMessage = nil }
            }

            Menu {
                Button {
                    showFileImporter = true
                } label: {
                    Label("Elegir archivo", systemImage: "folder")
                }

                if CameraPicker.isAvailable {
                    Button {
                        showCamera = true
                    } label: {
                        Label("Tomar foto", systemImage: "camera")
                    }
                }
            } label: {
                HStack(spacing: Theme.Spacing.xs) {
                    if isPreparing {
                        ProgressView().controlSize(.small)
                    } else {
                        Image(systemName: "paperclip")
                    }
                    Text(title)
                }
                .font(Theme.Font.footnote.weight(.semibold))
                .foregroundStyle(Theme.Color.primary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, Theme.Spacing.sm)
                .background(
                    RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                        .stroke(Theme.Color.primary.opacity(0.35),
                                style: StrokeStyle(lineWidth: 1.5, dash: [5, 4]))
                )
            }
            .disabled(isPreparing)

            PhotosPicker(selection: $photoItem, matching: .images) {
                Label("Elegir de mis fotos", systemImage: "photo.on.rectangle")
                    .font(Theme.Font.caption.weight(.medium))
                    .foregroundStyle(Theme.Color.primary)
            }
            .disabled(isPreparing)
        }
        .onChange(of: photoItem) { _, newValue in
            guard let newValue else { return }
            Task { await handlePhoto(newValue) }
        }
        .fullScreenCover(isPresented: $showCamera) {
            CameraPicker { image in
                Task { await handle(image: image, filename: "foto.jpg") }
            }
            .ignoresSafeArea()
        }
        .fileImporter(
            isPresented: $showFileImporter,
            allowedContentTypes: [.pdf, .png, .jpeg, .webP],
            allowsMultipleSelection: false
        ) { result in
            switch result {
            case let .success(urls):
                guard let url = urls.first else { return }
                Task { await handleFile(url) }
            case let .failure(error):
                errorMessage = error.localizedDescription
            }
        }
    }

    private func handlePhoto(_ item: PhotosPickerItem) async {
        isPreparing = true
        defer {
            isPreparing = false
            photoItem = nil
        }
        guard let data = try? await item.loadTransferable(type: Data.self),
              let image = UIImage(data: data)
        else {
            errorMessage = "No pudimos leer la imagen seleccionada."
            return
        }
        await handle(image: image, filename: "imagen.jpg")
    }

    private func handle(image: UIImage, filename: String) async {
        isPreparing = true
        defer { isPreparing = false }
        do {
            let prepared = try AttachmentPreparer.prepare(image: image, filename: filename)
            await onPrepared(prepared)
        } catch {
            errorMessage = ErrorPresenter.message(for: error)
        }
    }

    private func handleFile(_ url: URL) async {
        isPreparing = true
        defer { isPreparing = false }
        do {
            let prepared = try AttachmentPreparer.prepare(fileAt: url)
            await onPrepared(prepared)
        } catch {
            errorMessage = ErrorPresenter.message(for: error)
        }
    }
}
