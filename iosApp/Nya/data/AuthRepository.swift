import Foundation

struct AuthUser: Hashable, Sendable {
    var name: String
    var email: String
    var verified: Bool
    var isNew: Bool
}
