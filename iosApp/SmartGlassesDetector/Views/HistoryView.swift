import SwiftData
import SwiftUI

struct HistoryView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \DetectionRecord.detectedAt, order: .reverse) private var records: [DetectionRecord]
    @State private var confirmClear = false

    var body: some View {
        NavigationStack {
            Group {
                if records.isEmpty {
                    ContentUnavailableView(
                        "まだ見つかっていません",
                        systemImage: "magnifyingglass",
                        description: Text("「さがす」ボタンを押してみましょう！")
                    )
                } else {
                    List {
                        ForEach(grouped, id: \.title) { section in
                            Section(section.title) {
                                ForEach(section.records) { record in
                                    DetectionRow(
                                        name: record.name,
                                        manufacturerName: record.manufacturerName,
                                        distanceLabel: record.distanceLabel,
                                        detectedAt: record.detectedAt
                                    )
                                    .listRowBackground(AppTheme.surface)
                                    .listRowSeparator(.hidden)
                                    .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                                }
                            }
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
            .background(AppTheme.background.ignoresSafeArea())
            .navigationTitle("見つけた記録")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        confirmClear = true
                    } label: {
                        Image(systemName: "trash")
                    }
                    .accessibilityLabel("検出記録を削除")
                    .disabled(records.isEmpty)
                }
            }
            .alert("記録を削除しますか？", isPresented: $confirmClear) {
                Button("削除する", role: .destructive) {
                    model.clearRecords(context: modelContext)
                }
                Button("やめる", role: .cancel) {}
            } message: {
                Text("見つけた記録をこの端末から削除します。この操作は取り消せません。")
            }
        }
    }

    private var grouped: [(title: String, records: [DetectionRecord])] {
        let calendar = Calendar.current
        var today: [DetectionRecord] = []
        var yesterday: [DetectionRecord] = []
        var older: [DetectionRecord] = []
        for record in records {
            if calendar.isDateInToday(record.detectedAt) {
                today.append(record)
            } else if calendar.isDateInYesterday(record.detectedAt) {
                yesterday.append(record)
            } else {
                older.append(record)
            }
        }
        return [
            ("今日", today),
            ("昨日", yesterday),
            ("それ以前", older)
        ].filter { !$0.records.isEmpty }
    }
}
