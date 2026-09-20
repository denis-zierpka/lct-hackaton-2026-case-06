package ru.finny.pet.game.ui

import android.graphics.Typeface
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.text.Editable
import android.text.TextWatcher
import ru.finny.pet.R

/**
 * A plain EditText inside the game panel. Unlike the Compose field it asks the keyboard for
 * no fullscreen/extract mode, which otherwise hijacks the whole screen in landscape.
 */
@Composable
fun GameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    number: Boolean = false,
    maxLength: Int = 12,
    big: Boolean = false,
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(G.paperTint, RoundedCornerShape(16.dp))
            .border(2.dp, G.lavender, RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp),
        factory = { ctx ->
            EditText(ctx).apply {
                background = null
                hint.let { setHint(it) }
                setTextColor(G.ink.toArgb()); setHintTextColor(G.inkSoft.toArgb())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, if (big) 24f else 18f)
                typeface = runCatching { ResourcesCompat.getFont(ctx, R.font.montserrat) }.getOrNull() ?: Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER_VERTICAL
                isSingleLine = true
                inputType = if (number) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
                filters = arrayOf(InputFilter.LengthFilter(maxLength))
                setText(value)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) { onValueChange(s?.toString() ?: "") }
                })
            }
        },
        update = { view -> if (view.text.toString() != value) { view.setText(value); view.setSelection(value.length) } },
    )
}
