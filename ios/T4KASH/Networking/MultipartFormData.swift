import Foundation

/// Construye cuerpos `multipart/form-data`.
///
/// El backend recibe los adjuntos con `@RequestParam("file") MultipartFile`
/// (`AttachmentController`, `StudentVerificationController`), así que el nombre
/// de la parte debe ser exactamente `file`.
struct MultipartFormData {
    let boundary: String
    private var body = Data()

    init(boundary: String = "T4KASH-\(UUID().uuidString)") {
        self.boundary = boundary
    }

    var contentType: String { "multipart/form-data; boundary=\(boundary)" }

    mutating func addField(name: String, value: String) {
        body.append("--\(boundary)\r\n")
        body.append("Content-Disposition: form-data; name=\"\(name)\"\r\n\r\n")
        body.append("\(value)\r\n")
    }

    mutating func addFile(
        name: String,
        filename: String,
        mimeType: String,
        data: Data
    ) {
        body.append("--\(boundary)\r\n")
        body.append(
            "Content-Disposition: form-data; name=\"\(name)\"; filename=\"\(filename)\"\r\n"
        )
        body.append("Content-Type: \(mimeType)\r\n\r\n")
        body.append(data)
        body.append("\r\n")
    }

    /// Cierra el cuerpo. Debe llamarse una sola vez, al final.
    func finalized() -> Data {
        var closed = body
        closed.append("--\(boundary)--\r\n")
        return closed
    }
}

private extension Data {
    mutating func append(_ string: String) {
        if let data = string.data(using: .utf8) { append(data) }
    }
}
