import Foundation

/// Codificación JSON compartida por toda la aplicación.
///
/// El backend usa Jackson con `LocalDateTime`, es decir ISO-8601 **sin zona**.
/// Se interpreta en la zona del dispositivo para que iOS y Android muestren la
/// misma hora a partir del mismo dato.
enum JSONCoding {
    static let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .custom { decoder in
            let container = try decoder.singleValueContainer()
            let raw = try container.decode(String.self)
            guard let date = APIDateFormatter.date(from: raw) else {
                throw DecodingError.dataCorruptedError(
                    in: container,
                    debugDescription: "Fecha con formato inesperado: \(raw)"
                )
            }
            return date
        }
        return decoder
    }()

    static let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .custom { date, encoder in
            var container = encoder.singleValueContainer()
            try container.encode(APIDateFormatter.string(from: date))
        }
        return encoder
    }()
}
