import SwiftUI

/// Creación y edición de publicaciones del feed.
struct PostComposerSheet: View {
    enum Mode {
        case create
        case edit(Post)

        var isEditing: Bool {
            if case .edit = self { return true }
            return false
        }
    }

    let mode: Mode
    let onSubmit: (
        String,
        Domain.TipoPublicacion,
        Domain.VisibilidadPublicacion,
        Bool
    ) async -> Bool

    @Environment(\.dismiss) private var dismiss

    @State private var contenido = ""
    @State private var tipo: Domain.TipoPublicacion = .texto
    @State private var visibilidad: Domain.VisibilidadPublicacion = .publica
    @State private var permiteComentarios = true

    /// `CreatePostRequest.contenido` admite hasta 5000 caracteres.
    private var isValid: Bool {
        let trimmed = contenido.trimmingCharacters(in: .whitespacesAndNewlines)
        return !trimmed.isEmpty && trimmed.count <= 5_000
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextEditor(text: $contenido)
                        .frame(minHeight: 160)
                } header: {
                    Text("Contenido")
                } footer: {
                    Text("\(contenido.count)/5000")
                        .foregroundStyle(contenido.count > 5_000
                                         ? Theme.Color.danger
                                         : Theme.Color.textSoft)
                }

                Section("Tipo de publicación") {
                    Picker("Tipo", selection: $tipo) {
                        ForEach(Domain.TipoPublicacion.composable) { option in
                            Label(option.label, systemImage: option.icon).tag(option)
                        }
                    }
                    .pickerStyle(.navigationLink)
                }

                Section {
                    Picker("Visibilidad", selection: $visibilidad) {
                        ForEach(Domain.VisibilidadPublicacion.allCases) { option in
                            Label(option.label, systemImage: option.icon).tag(option)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                } header: {
                    Text("¿Quién puede verla?")
                }

                Section {
                    Toggle("Permitir comentarios", isOn: $permiteComentarios)
                }

                Section {
                    AsyncButton {
                        if await onSubmit(contenido, tipo, visibilidad, permiteComentarios) {
                            dismiss()
                        }
                    } label: {
                        Text(mode.isEditing ? "Guardar cambios" : "Publicar")
                            .frame(maxWidth: .infinity)
                    }
                    .disabled(!isValid)
                }
            }
            .navigationTitle(mode.isEditing ? "Editar publicación" : "Nueva publicación")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
            }
            .onAppear(perform: prefillIfEditing)
        }
    }

    private func prefillIfEditing() {
        guard case let .edit(post) = mode, contenido.isEmpty else { return }
        contenido = post.contenido ?? ""
        tipo = post.tipo ?? .texto
        visibilidad = post.visibilidadResuelta ?? .publica
        permiteComentarios = post.permiteComentarios
    }
}
