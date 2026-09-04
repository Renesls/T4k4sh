import SwiftUI

/// Billetera: balance, pagos, movimientos, disputas, reembolsos y desembolsos.
///
/// Los saldos los calcula el backend a partir de movimientos verificables
/// (`WalletResponse`), así que aquí solo se presentan.
struct WalletView: View {
    @Environment(AppDependencies.self) private var dependencies

    @State private var state: LoadState<Wallet> = .idle
    @State private var section: Section = .movimientos
    @State private var actionError: String?

    enum Section: String, CaseIterable, Identifiable {
        case movimientos = "Movimientos"
        case pagos = "Pagos"
        case disputas = "Disputas"

        var id: String { rawValue }
    }

    var body: some View {
        Group {
            switch state {
            case .idle, .loading:
                LoadingStateView(message: "Cargando tu billetera…")

            case let .failed(message):
                ErrorStateView(message: message) { await load() }

            case let .loaded(wallet):
                ScrollView {
                    VStack(spacing: Theme.Spacing.md) {
                        if let actionError {
                            InlineErrorBanner(message: actionError) { self.actionError = nil }
                        }

                        balanceCard(wallet)

                        Picker("Sección", selection: $section) {
                            ForEach(Section.allCases) { option in
                                Text(option.rawValue).tag(option)
                            }
                        }
                        .pickerStyle(.segmented)

                        switch section {
                        case .movimientos: movementsSection(wallet)
                        case .pagos: paymentsSection(wallet)
                        case .disputas: disputesSection(wallet)
                        }
                    }
                    .padding(Theme.Spacing.md)
                }
                .refreshable { await load() }
            }
        }
        .screenBackground()
        .navigationTitle("Billetera")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    // MARK: - Secciones

    private func balanceCard(_ wallet: Wallet) -> some View {
        VStack(spacing: Theme.Spacing.md) {
            VStack(spacing: 4) {
                Text("Balance disponible")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textMuted)
                Text(DisplayFormatter.money(wallet.balanceDisponible, currencyCode: wallet.moneda))
                    .font(.system(.largeTitle, design: .rounded).weight(.bold).monospacedDigit())
                    .foregroundStyle(Theme.Color.primaryDark)
            }

            Divider().overlay(Theme.Color.border)

            HStack {
                walletMetric(
                    title: "Retenido",
                    value: DisplayFormatter.money(wallet.fondosRetenidos, currencyCode: wallet.moneda),
                    icon: "lock.shield",
                    tint: Theme.Color.warning
                )
                Divider().frame(height: 36).overlay(Theme.Color.border)
                walletMetric(
                    title: "Total ganado",
                    value: DisplayFormatter.money(wallet.totalGanado, currencyCode: wallet.moneda),
                    icon: "chart.line.uptrend.xyaxis",
                    tint: Theme.Color.success
                )
            }

            if !wallet.desembolsos.isEmpty {
                Divider().overlay(Theme.Color.border)
                DetailRow(
                    label: "Desembolsos registrados",
                    value: "\(wallet.desembolsos.count)",
                    icon: "arrow.up.right.circle"
                )
            }
        }
        .cardSurface()
    }

    private func walletMetric(
        title: String,
        value: String,
        icon: String,
        tint: Color
    ) -> some View {
        VStack(spacing: 4) {
            Label(title, systemImage: icon)
                .font(Theme.Font.caption)
                .foregroundStyle(Theme.Color.textMuted)
            Text(value)
                .font(Theme.Font.subheadline.monospacedDigit())
                .foregroundStyle(tint)
        }
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private func movementsSection(_ wallet: Wallet) -> some View {
        if wallet.movimientos.isEmpty {
            EmptyStateView(
                icon: "list.bullet.rectangle",
                title: "Sin movimientos todavía",
                message: "Aquí verás cada retención, liberación y reembolso de tus trabajos."
            )
        } else {
            VStack(spacing: Theme.Spacing.xs) {
                ForEach(wallet.movimientos) { movement in
                    HStack(alignment: .top, spacing: Theme.Spacing.sm) {
                        Image(systemName: movement.afectaDisponible
                              ? "arrow.down.circle.fill"
                              : "lock.circle.fill")
                            .font(.title3)
                            .foregroundStyle(movement.afectaDisponible
                                             ? Theme.Color.success
                                             : Theme.Color.warning)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(movement.tipoMovimiento.humanizedCode)
                                .font(Theme.Font.footnote.weight(.semibold))
                                .foregroundStyle(Theme.Color.text)
                            if let descripcion = movement.descripcion, !descripcion.isEmpty {
                                Text(descripcion)
                                    .font(Theme.Font.caption)
                                    .foregroundStyle(Theme.Color.textMuted)
                            }
                            Text(DisplayFormatter.dateTime(movement.fechaRegistro))
                                .font(Theme.Font.caption)
                                .foregroundStyle(Theme.Color.textSoft)
                        }

                        Spacer(minLength: 0)

                        Text(DisplayFormatter.money(movement.monto, currencyCode: movement.moneda))
                            .font(Theme.Font.subheadline.monospacedDigit())
                            .foregroundStyle(Theme.Color.text)
                    }
                    .cardSurface(padding: Theme.Spacing.sm)
                }
            }
        }
    }

    @ViewBuilder
    private func paymentsSection(_ wallet: Wallet) -> some View {
        if wallet.pagos.isEmpty {
            EmptyStateView(
                icon: "creditcard",
                title: "Sin pagos registrados",
                message: "Cuando aceptes o recibas un trabajo con pago aparecerá aquí."
            )
        } else {
            VStack(spacing: Theme.Spacing.xs) {
                ForEach(wallet.pagos) { payment in
                    VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                        HStack {
                            Label(
                                payment.esEfectivo ? "Efectivo" : "Pago protegido",
                                systemImage: payment.esEfectivo ? "banknote" : "lock.shield"
                            )
                            .font(Theme.Font.footnote.weight(.semibold))
                            .foregroundStyle(Theme.Color.text)

                            Spacer(minLength: 0)

                            StatusPill(
                                text: payment.estadoLegible,
                                tint: StatusTint.payment(payment.estadoPago)
                            )
                        }

                        DetailRow(
                            label: "Monto al estudiante",
                            value: DisplayFormatter.money(
                                payment.montoEstudiante,
                                currencyCode: payment.monedaCobro
                            ),
                            icon: "person.badge.key"
                        )

                        DetailRow(
                            label: "Actualizado",
                            value: DisplayFormatter.dateTime(payment.fechaActualizacion),
                            icon: "clock"
                        )
                    }
                    .cardSurface(padding: Theme.Spacing.sm)
                }
            }
        }
    }

    @ViewBuilder
    private func disputesSection(_ wallet: Wallet) -> some View {
        if wallet.disputas.isEmpty && wallet.reembolsos.isEmpty {
            EmptyStateView(
                icon: "checkmark.shield",
                title: "Sin disputas abiertas",
                message: "Si algo sale mal con un pago retenido, puedes abrir una disputa desde el trabajo."
            )
        } else {
            VStack(spacing: Theme.Spacing.xs) {
                ForEach(wallet.disputas) { dispute in
                    VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                        HStack {
                            Text(dispute.motivo)
                                .font(Theme.Font.footnote.weight(.semibold))
                                .foregroundStyle(Theme.Color.text)
                            Spacer(minLength: 0)
                            StatusPill(
                                text: dispute.estadoDisputa.humanizedCode,
                                tint: dispute.resuelta ? Theme.Color.success : Theme.Color.warning
                            )
                        }

                        Text(dispute.descripcion)
                            .font(Theme.Font.caption)
                            .foregroundStyle(Theme.Color.textMuted)

                        DetailRow(
                            label: "Solución solicitada",
                            value: dispute.solucionLegible,
                            icon: "arrow.triangle.branch"
                        )
                        DetailRow(
                            label: "Monto disputado",
                            value: DisplayFormatter.money(dispute.montoDisputado),
                            icon: "banknote"
                        )

                        if let resolucion = dispute.resolucion, !resolucion.isEmpty {
                            Divider().overlay(Theme.Color.border)
                            Text("Resolución: \(resolucion)")
                                .font(Theme.Font.caption)
                                .foregroundStyle(Theme.Color.text)
                        }
                    }
                    .cardSurface(padding: Theme.Spacing.sm)
                }

                ForEach(wallet.reembolsos) { refund in
                    HStack {
                        Label("Reembolso", systemImage: "arrow.uturn.left.circle")
                            .font(Theme.Font.footnote.weight(.semibold))
                            .foregroundStyle(Theme.Color.text)
                        Spacer(minLength: 0)
                        Text(DisplayFormatter.money(refund.montoReembolso, currencyCode: refund.moneda))
                            .font(Theme.Font.subheadline.monospacedDigit())
                            .foregroundStyle(Theme.Color.text)
                        StatusPill(text: refund.estadoReembolso.humanizedCode, tint: Theme.Color.textMuted)
                    }
                    .cardSurface(padding: Theme.Spacing.sm)
                }
            }
        }
    }

    private func load() async {
        if state.value == nil { state = .loading }
        do {
            state = .loaded(try await dependencies.finance.wallet())
        } catch {
            state = .failed(ErrorPresenter.message(for: error))
        }
    }
}

/// Formulario para abrir una disputa sobre un pago retenido.
struct DisputeComposerSheet: View {
    let onSubmit: (String, String, Domain.SolucionDisputa) async -> Bool

    @Environment(\.dismiss) private var dismiss
    @State private var motivo = ""
    @State private var descripcion = ""
    @State private var solucion: Domain.SolucionDisputa = .reembolsoCliente

    /// `CreatePaymentDisputeRequest`: motivo ≤ 120, descripción entre 10 y 1000.
    private var isValid: Bool {
        let motivoLimpio = motivo.trimmingCharacters(in: .whitespaces)
        let descripcionLimpia = descripcion.trimmingCharacters(in: .whitespaces)
        return !motivoLimpio.isEmpty
            && motivoLimpio.count <= 120
            && (10...1_000).contains(descripcionLimpia.count)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Ej. La entrega no corresponde a lo acordado", text: $motivo)
                } header: {
                    Text("Motivo")
                } footer: {
                    Text("\(motivo.count)/120")
                }

                Section {
                    TextEditor(text: $descripcion)
                        .frame(minHeight: 140)
                } header: {
                    Text("Descripción")
                } footer: {
                    Text("\(descripcion.count)/1000 · mínimo 10 caracteres. "
                         + "Describe lo ocurrido con el mayor detalle posible.")
                }

                Section {
                    Picker("Solución solicitada", selection: $solucion) {
                        ForEach(Domain.SolucionDisputa.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                } header: {
                    Text("¿Qué esperas que ocurra?")
                } footer: {
                    Text("Un administrador revisará el caso y decidirá liberando el pago o reembolsando.")
                }

                Section {
                    AsyncButton {
                        if await onSubmit(motivo, descripcion, solucion) { dismiss() }
                    } label: {
                        Text("Abrir disputa")
                            .frame(maxWidth: .infinity)
                    }
                    .disabled(!isValid)
                }
            }
            .navigationTitle("Abrir disputa")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
            }
        }
    }
}
