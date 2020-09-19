package app.spidy.memecreator.data

data class Tool(
    val tag: String,
    val title: String,
    val drawable: Int
) {
    companion object {
        const val BRUSH = "app.spidy.mime.BRUSH"
        const val ERASER = "app.spidy.mime.ERASER"
        const val EDITOR_BACKGROUND_COLOR = "app.spidy.mime.EDITOR_BACKGROUND_COLOR"
        const val EDITOR_IMAGE = "app.spidy.mime.EDITOR_IMAGE"
        const val EDITOR_PADDING = "app.spidy.mime.EDITOR_PADDING"

        const val TEXT_EDIT = "app.spidy.mime.TEXT_EDIT"
        const val TEXT_COLOR = "app.spidy.mime.TEXT_COLOR"
        const val TEXT_SIZE = "app.spidy.mime.TEXT_SIZE"
        const val TEXT_FONT = "app.spidy.mime.TEXT_FONT"
        const val TEXT_OUTLINE = "app.spidy.mime.TEXT_OUTLINE"
        const val TEXT_OPACITY = "app.spidy.mime.TEXT_OPACITY"

        const val IMAGE_CHANGE = "app.spidy.mime.IMAGE_CHANGE"
        const val IMAGE_OPACITY = "app.spidy.mime.IMAGE_OPACITY"
        const val IMAGE_ROTATE = "app.spidy.mime.IMAGE_ROTATE_LEFT"

        const val EMOJI_OPACITY = "app.spidy.mime.EMOJI_OPACITY"
    }
}