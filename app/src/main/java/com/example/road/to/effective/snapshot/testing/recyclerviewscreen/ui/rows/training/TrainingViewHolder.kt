package com.example.road.to.effective.snapshot.testing.recyclerviewscreen.ui.rows.training

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.doOnNextLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.road.to.effective.snapshot.testing.R
import com.example.road.to.effective.snapshot.testing.recyclerviewscreen.LanguageFilterClickedListener
import com.example.road.to.effective.snapshot.testing.recyclerviewscreen.TrainAllClickedListener
import com.example.road.to.effective.snapshot.testing.recyclerviewscreen.data.Language
import com.example.road.to.effective.snapshot.testing.recyclerviewscreen.data.Translation
import com.example.road.to.effective.snapshot.testing.recyclerviewscreen.utils.px
import com.example.road.to.effective.snapshot.testing.recyclerviewscreen.utils.sortByValueSize
import com.example.road.to.effective.snapshot.testing.recyclerviewscreen.ui.views.DigitTextView
import com.example.road.to.effective.snapshot.testing.recyclerviewscreen.ui.views.LanguageFilterView
import com.example.road.to.effective.snapshot.testing.recyclerviewscreen.ui.views.LanguageRadioGroup
import com.example.road.to.effective.snapshot.testing.recyclerviewscreen.ui.views.LongAsShortTransformer

class TrainingViewHolder(
    private val container: View
) : RecyclerView.ViewHolder(container) {
    val contextWrapper =
        ContextThemeWrapper(container.context, R.style.FlagCheckMark)

    private var filterClickedListener: LanguageFilterClickedListener? = null
    private var trainAllClickedListener: TrainAllClickedListener? = null

    fun <T> bind(
        item: TrainingItem,
        languageClickedListener: T?
    ) where T : TrainAllClickedListener,
            T : LanguageFilterClickedListener {

        filterClickedListener = languageClickedListener
        trainAllClickedListener = languageClickedListener

        with(container.findViewById<LanguageRadioGroup>(R.id.radioGroup)) {
            removeAllViews()
            initFilterView(contextWrapper, item, languageClickedListener)
            calculateRadioGroupMinimumHeight()
        }

        with(container.findViewById<DigitTextView>(R.id.amountText)) {
            longTransformer = LongAsShortTransformer(container.context)
            setAnimationOff()
            setValue(item.getWordsToMemoriseAmount())
            setAnimationOn()
        }

        container.animateEmptyStateTransition(item.trainingByLang.isEmpty())

        with(container.findViewById<Button>(R.id.trainButton)) {
            setOnClickListener { trainAllClickedListener?.onTrainAllClicked() }
            isEnabled = item.trainingByLang.isNotEmpty()
            alpha = if (isEnabled) 1f else 0.5f
        }
    }

    fun update(payload: TrainingItemPayload) {
        val oldLangsSorted = payload.oldTrainingItem.trainingByLang.sortByValueSize()
        val newLangsSorted = payload.newTrainingItem.trainingByLang.sortByValueSize()

        container.animateEmptyStateTransition(newLangsSorted.isEmpty())

        with(container.findViewById<LanguageRadioGroup>(R.id.radioGroup)) {
            animateFilterAmounts(payload.newTrainingItem, oldLangsSorted.keys, newLangsSorted.keys)
            animateFilters(
                contextWrapper,
                payload.newTrainingItem,
                filterClickedListener,
                oldLangsSorted,
                newLangsSorted
            )
        }

        with(container.findViewById<Button>(R.id.trainButton)) {
            isEnabled = payload.newTrainingItem.activeLangs.isNotEmpty()
            alpha = if (isEnabled) 1f else 0.5f
        }

        with(container.findViewById<DigitTextView>(R.id.amountText)) {
            longTransformer = LongAsShortTransformer(context)
            setValue(payload.newTrainingItem.getWordsToMemoriseAmount())
        }
    }
}

private fun RadioGroup.initFilterView(
    context: Context, item: TrainingItem, listener: LanguageFilterClickedListener?,
) {
    item.trainingByLang.sortByValueSize().keys
        .forEach { lang ->
            val checkMark =
                LanguageFilterView.createLanguageRadioButtonLayout(
                    ctx = context,
                    checked = item.activeLangs.contains(lang),
                    viewTag = lang,
                    amountTexts = item.trainingByLang[lang]?.size ?: 0,
                    listener = listener,
                )

            addView(checkMark)
        }
}

private fun RadioGroup.calculateRadioGroupMinimumHeight() {
    doOnNextLayout {
        minimumHeight = when {
            height != 0 -> height
            else -> 80.px()
        }
    }
}

private fun View.animateEmptyStateTransition(showEmptyState: Boolean) {
    val fadingOutDuration = 600L
    val fadingInDuration = 1_000L
    val invisibleAlpha = 0f
    val visibleAlpha = 1f

    val filterText = findViewById<TextView>(R.id.filterText)
    val emptyMemorisesImage = findViewById<ImageView>(R.id.emptyMemorisesImage)

    when {
        showEmptyState -> {
            filterText.animate().setDuration(fadingOutDuration).alpha(invisibleAlpha)
            emptyMemorisesImage.animate().setDuration(fadingInDuration).alpha(visibleAlpha)
        }
        else -> {
            filterText.animate().setDuration(fadingInDuration).alpha(visibleAlpha)
            emptyMemorisesImage.animate().setDuration(fadingOutDuration)
                .alpha(invisibleAlpha)
        }
    }
}

private fun TrainingItem.getWordsToMemoriseAmount(): Long =
    trainingByLang
        .filterKeys { activeLangs.contains(it) }
        .values.fold(emptyList<Translation>()) { acc, list -> acc + list }
        .size
        .toLong()

private fun LanguageRadioGroup.animateFilterAmounts(
    newTrainingItem: TrainingItem, oldLangsKeys: Set<Language>, newLangsKeys: Set<Language>
) {
    newLangsKeys.intersect(oldLangsKeys).forEach {
        val newValue = newTrainingItem.trainingByLang[it]?.size ?: 0
        getLangRadioButtonByTag(it)?.setValue(newValue)
    }
}

private fun LanguageRadioGroup.animateFilters(
    context: Context,
    item: TrainingItem,
    listener: LanguageFilterClickedListener?,
    oldLangsSorted: Map<Language, Collection<Translation>>,
    newLangsSorted: Map<Language, Collection<Translation>>
) {
    val oldLangsKeys = oldLangsSorted.keys
    val newLangsKeys = newLangsSorted.keys

    //lists also compare item positions for equals()
    if (oldLangsKeys.toList() != newLangsKeys.toList()) {
        if (oldLangsKeys.size > newLangsKeys.size) {
            val langsToRemove = oldLangsKeys.subtract(newLangsKeys)
            removeLangs(langsToRemove)
        }

        if (oldLangsSorted.size < newLangsKeys.size) {
            val langsToAdd = newLangsKeys.subtract(oldLangsKeys)
            val langsToAddWithTranslations =
                oldLangsSorted.filterKeys { langsToAdd.contains(it) }

            addNewLangs(
                context = context,
                allLangsSorted = newLangsKeys.toSortedSet(),
                langsToAdd = langsToAddWithTranslations,
                activeLangs = item.activeLangs,
                listener = listener
            )

        } else {
            reorderLangs(
                oldLangsOrder = linkedSetOf(*oldLangsKeys.toTypedArray()),
                newLangsOrder = linkedSetOf(*newLangsKeys.toTypedArray())
            )
        }
    }
}

