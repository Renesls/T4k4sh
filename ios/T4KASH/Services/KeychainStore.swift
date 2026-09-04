import Foundation
import Security

/// Acceso al Keychain para los datos sensibles de la sesión.
///
/// Sustituye a `SecureTokenStore` de Android (AES/GCM sobre AndroidKeyStore).
/// El token nunca se guarda en `UserDefaults` ni en archivos de la app.
enum KeychainStore {
    /// Errores del Keychain, con el `OSStatus` original para diagnóstico.
    enum KeychainError: Error {
        case unexpectedStatus(OSStatus)
    }

    private static let service = "com.t4kash.app.session"

    /// Guarda o reemplaza un valor.
    ///
    /// `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` permite que la app siga
    /// funcionando tras un reinicio sin desbloqueo manual, e impide que el token
    /// viaje en copias de seguridad a otro dispositivo.
    @discardableResult
    static func save(_ value: String, account: String) -> Bool {
        guard let data = value.data(using: .utf8) else { return false }

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        let attributes: [String: Any] = [
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]

        let updateStatus = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if updateStatus == errSecSuccess { return true }

        guard updateStatus == errSecItemNotFound else { return false }

        var insert = query
        insert.merge(attributes) { current, _ in current }
        return SecItemAdd(insert as CFDictionary, nil) == errSecSuccess
    }

    static func read(account: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]

        var item: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
              let data = item as? Data,
              let value = String(data: data, encoding: .utf8),
              !value.isEmpty
        else { return nil }

        return value
    }

    @discardableResult
    static func delete(account: String) -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }
}
