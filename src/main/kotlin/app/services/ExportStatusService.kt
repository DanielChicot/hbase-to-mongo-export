package app.services

interface ExportStatusService {
    fun incrementExportedCount(exportedFile: String)
    fun setExportedStatus()
    fun setFailedStatus()
    fun setTableUnavailableStatus()
    fun setBlockedTopicStatus()
}
