import Foundation
import UIKit
import UniformTypeIdentifiers

/// Archivo listo para subir al backend.
struct PreparedAttachment: Identifiable, Equatable {
    let id = UUID()
    let filename: String
    let mimeType: String
    let data: Data

    var sizeBytes: Int64 { Int64(data.count) }
}

/// Prepara imágenes y documentos antes de enviarlos.
///
/// El backend limita cada archivo a 10 MB y solo acepta ciertas extensiones
/// (`AttachmentService.ALLOWED_EXTENSIONS`), así que la validación y la
/// compresión se hacen aquí, antes de gastar la subida.
enum AttachmentPreparer {
    /// Error legible para mostrar sin traducciones adicionales.
    enum PreparationError: LocalizedError {
        case tooLarge(Int64)
        case unreadable
        case unsupportedType(String)

        var errorDescription: String? {
            switch self {
            case let .tooLarge(size):
                "El archivo pesa \(DisplayFormatter.fileSize(size)) y el límite es "
                    + "\(DisplayFormatter.fileSize(Int64(AppConfig.maxAttachmentBytes)))."
            case .unreadable:
                "No pudimos leer el archivo seleccionado."
            case let .unsupportedType(ext):
                "El formato «\(ext)» no está permitido para adjuntos."
            }
        }
    }

    /// Extensiones aceptadas por `AttachmentService` en el backend.
    static let allowedExtensions: Set<String> = ["pdf", "png", "jpg", "jpeg", "webp"]

    /// Comprime una imagen a JPEG buscando quedar por debajo del límite del backend.
    static func prepare(image: UIImage, filename: String = "adjunto.jpg") throws -> PreparedAttachment {
        // Redimensiona antes de comprimir: una foto de 12 MP no aporta nada como
        // comprobante y multiplica el peso.
        let resized = image.resized(maxDimension: 2_048)

        var quality: CGFloat = 0.8
        var data = resized.jpegData(compressionQuality: quality)

        while let current = data,
              current.count > AppConfig.maxAttachmentBytes,
              quality > 0.3 {
            quality -= 0.15
            data = resized.jpegData(compressionQuality: quality)
        }

        guard let finalData = data else { throw PreparationError.unreadable }
        guard finalData.count <= AppConfig.maxAttachmentBytes else {
            throw PreparationError.tooLarge(Int64(finalData.count))
        }

        return PreparedAttachment(
            filename: filename.hasSuffix(".jpg") ? filename : filename + ".jpg",
            mimeType: "image/jpeg",
            data: finalData
        )
    }

    /// Valida y carga un documento elegido con el selector de archivos.
    static func prepare(fileAt url: URL) throws -> PreparedAttachment {
        let ext = url.pathExtension.lowercased()
        guard allowedExtensions.contains(ext) else {
            throw PreparationError.unsupportedType(ext.isEmpty ? "desconocido" : ext)
        }

        // Los archivos que entrega el selector viven fuera del sandbox de la app.
        let needsScope = url.startAccessingSecurityScopedResource()
        defer { if needsScope { url.stopAccessingSecurityScopedResource() } }

        guard let data = try? Data(contentsOf: url) else {
            throw PreparationError.unreadable
        }
        guard data.count <= AppConfig.maxAttachmentBytes else {
            throw PreparationError.tooLarge(Int64(data.count))
        }

        let mimeType = UTType(filenameExtension: ext)?.preferredMIMEType
            ?? "application/octet-stream"

        return PreparedAttachment(
            filename: url.lastPathComponent,
            mimeType: mimeType,
            data: data
        )
    }
}

extension UIImage {
    /// Escala manteniendo la proporción, sin agrandar imágenes ya pequeñas.
    func resized(maxDimension: CGFloat) -> UIImage {
        let longestSide = max(size.width, size.height)
        guard longestSide > maxDimension else { return self }

        let scale = maxDimension / longestSide
        let target = CGSize(width: size.width * scale, height: size.height * scale)

        let renderer = UIGraphicsImageRenderer(size: target)
        return renderer.image { _ in
            draw(in: CGRect(origin: .zero, size: target))
        }
    }
}
