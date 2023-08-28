/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.ui.actionsheet

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.EXTRA_VIEW_HEADERS
import ch.protonmail.android.activities.messageDetails.MessageViewHeadersActivity
import ch.protonmail.android.activities.messageDetails.viewmodel.MessageDetailsViewModel
import ch.protonmail.android.core.Constants
import ch.protonmail.android.databinding.FragmentMessageActionSheetBinding
import ch.protonmail.android.databinding.LayoutMessageDetailsActionsSheetButtonsBinding
import ch.protonmail.android.details.presentation.ui.MessageDetailsActivity
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import ch.protonmail.android.mailbox.presentation.viewmodel.MailboxViewModel
import ch.protonmail.android.ui.actionsheet.model.ActionSheetTarget
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showDeleteConfirmationDialog
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showTwoButtonInfoDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Fragment popping up with actions for messages.
 */
@AndroidEntryPoint
class MessageActionSheet : BottomSheetDialogFragment() {

    private var actionSheetHeader: ActionSheetHeader? = null
    private val viewModel: MessageActionSheetViewModel by viewModels()
    private val mailboxViewModel: MailboxViewModel by activityViewModels()
    private val messageDetailsViewModel: MessageDetailsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val actionsTarget = arguments?.getSerializable(EXTRA_ARG_ACTION_TARGET) as? ActionSheetTarget
            ?: ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN
        val messageIds: List<String> = arguments?.getStringArray(EXTRA_ARG_MESSAGE_IDS)?.toList()
            ?: throw IllegalStateException("messageIds in MessageActionSheet are Empty!")
        val messageLocation =
            Constants.MessageLocationType.fromInt(
                arguments?.getInt(EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID) ?: 0
            )
        val mailboxLabelId = arguments?.getString(EXTRA_ARG_MAILBOX_LABEL_ID)
            ?: messageLocation.messageLocationTypeValue.toString()
        val doesConversationHaveMoreThanOneMessage =
            arguments?.getBoolean(EXTRA_ARG_CONVERSATION_HAS_MORE_THAN_ONE_MESSAGE) ?: true

        val isScheduled = arguments?.getBoolean(EXTRA_ARG_IS_SCHEDULED) ?: false

        Timber.v("MessageActionSheet for location: $messageLocation")
        val binding = FragmentMessageActionSheetBinding.inflate(inflater)

        viewModel.stateFlow
            .onEach { updateViewState(isScheduled, it, binding) }
            .launchIn(lifecycleScope)

        viewModel.setupViewState(
            requireContext(),
            messageIds,
            messageLocation,
            mailboxLabelId,
            actionsTarget
        )

        setupHeaderBindings(binding.actionSheetHeaderDetailsActions, arguments)
        setupReplyActionsBindings(
            binding.includeLayoutActionSheetButtons,
            actionsTarget,
            messageIds,
            isScheduled,
            doesConversationHaveMoreThanOneMessage
        )
        setupMoreSectionBindings(binding, actionsTarget, messageIds)
        actionSheetHeader = binding.actionSheetHeaderDetailsActions

        viewModel.actionsFlow
            .onEach { processAction(it) }
            .launchIn(lifecycleScope)

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState)
        bottomSheetDialog.setOnShowListener { dialogInterface ->
            val dialog = dialogInterface as BottomSheetDialog
            val bottomSheet: View? = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            val targetOffsetSize = resources.getDimensionPixelSize(R.dimen.padding_3xl)
            if (bottomSheet != null) {
                val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                bottomSheetBehavior.addBottomSheetCallback(
                    object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                            Timber.v("State changed to $newState")
                            if (newState == STATE_EXPANDED) {
                                setCloseIconVisibility(true)
                                if (bottomSheet.isExpandedToFullscreen()) {
                                    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                                }
                            } else {
                                setCloseIconVisibility(false)
                                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                            }
                        }

                        override fun onSlide(bottomSheet: View, slideOffset: Float) {
                            if (slideOffset > HEADER_SLIDE_THRESHOLD) {
                                val intermediateShift =
                                    targetOffsetSize *
                                        ((slideOffset - HEADER_SLIDE_THRESHOLD) * (1 / (1 - HEADER_SLIDE_THRESHOLD)))
                                actionSheetHeader?.shiftTitleToRightBy(intermediateShift)
                            } else {
                                actionSheetHeader?.shiftTitleToRightBy(0f)
                            }
                        }
                    }
                )
            }
        }
        return bottomSheetDialog
    }

    override fun onDestroyView() {
        actionSheetHeader = null
        super.onDestroyView()
    }

    private fun updateViewState(
        isScheduled: Boolean,
        state: MessageActionSheetState,
        binding: FragmentMessageActionSheetBinding
    ) {
        when (state) {
            is MessageActionSheetState.Data -> {
                setupManageSectionBindings(binding, state.manageSectionState)
                setupMoveSectionState(isScheduled, binding, state.moveSectionState)
            }
            MessageActionSheetState.Initial -> {
            }
        }
    }

    private fun setupHeaderBindings(
        actionSheetHeader: ActionSheetHeader,
        arguments: Bundle?
    ) {
        with(actionSheetHeader) {
            val title = arguments?.getString(EXTRA_ARG_TITLE)
            if (!title.isNullOrEmpty()) {
                setTitle(title)
            }
            val subtitle = arguments?.getString(EXTRA_ARG_SUBTITLE)
            if (!subtitle.isNullOrEmpty()) {
                setSubTitle(subtitle)
            }
            setOnCloseClickListener {
                dismiss()
            }
        }
    }

    private fun setupReplyActionsBindings(
        binding: LayoutMessageDetailsActionsSheetButtonsBinding,
        actionsTarget: ActionSheetTarget,
        messageIds: List<String>,
        isScheduled: Boolean,
        doesConversationHaveMoreThanOneMessage: Boolean
    ) {
        with(binding) {
            layoutDetailsActions.isVisible = !isScheduled && (
                actionsTarget in listOf(
                    ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN,
                    ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN
                ) || actionsTarget == ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN &&
                    !doesConversationHaveMoreThanOneMessage
                )

            textViewDetailsActionsReply.setOnClickListener {
                (activity as? MessageDetailsActivity)?.executeMessageAction(
                    Constants.MessageActionType.REPLY, messageIds.first()
                )
                dismiss()
            }
            textViewDetailsActionsReplyAll.setOnClickListener {
                (activity as? MessageDetailsActivity)?.executeMessageAction(
                    Constants.MessageActionType.REPLY_ALL, messageIds.first()
                )
                dismiss()
            }
            textViewDetailsActionsForward.setOnClickListener {
                (activity as? MessageDetailsActivity)?.executeMessageAction(
                    Constants.MessageActionType.FORWARD, messageIds.first()
                )
                dismiss()
            }
        }
    }

    private fun setupManageSectionBindings(
        binding: FragmentMessageActionSheetBinding,
        state: MessageActionSheetState.ManageSectionState
    ) {
        with(binding) {
            textViewDetailsActionsUnstar.apply {
                isVisible = state.showUnstarAction
                setOnClickListener {
                    viewModel.unStarMessage(
                        state.mailboxItemIds,
                        state.messageLocation,
                        state.actionsTarget == ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN
                    )
                }
            }

            textViewDetailsActionsStar.apply {
                isVisible = state.showStarAction
                setOnClickListener {
                    viewModel.starMessage(
                        state.mailboxItemIds,
                        state.messageLocation,
                        state.actionsTarget == ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN
                    )
                }
            }

            textViewDetailsActionsMarkRead.apply {
                isVisible = state.showMarkReadAction
                setOnClickListener {
                    viewModel.markRead(
                        state.mailboxItemIds,
                        state.messageLocation,
                        state.currentLocationId,
                        state.actionsTarget == ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN
                    )
                }
            }
            textViewDetailsActionsMarkUnread.setOnClickListener {
                viewModel.markUnread(
                    state.mailboxItemIds,
                    state.messageLocation,
                    state.currentLocationId,
                    state.actionsTarget == ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN
                )
            }
            textViewDetailsActionsLabelAs.setOnClickListener {
                viewModel.showLabelsManager(state.mailboxItemIds, state.messageLocation, state.currentLocationId)
                dismiss()
            }
            detailsActionsViewInLightModeTextView.apply {
                isVisible = state.showViewInLightModeAction
                setOnClickListener {
                    viewModel.viewInLightMode(state.mailboxItemIds.first())
                }
            }
            detailsActionsViewInDarkModeTextView.apply {
                isVisible = state.showViewInDarkModeAction
                setOnClickListener {
                    viewModel.viewInDarkMode(state.mailboxItemIds.first())
                }
            }
        }
    }

    private fun setupMoveSectionState(
        isScheduled: Boolean,
        binding: FragmentMessageActionSheetBinding,
        state: MessageActionSheetState.MoveSectionState
    ) {
        val messageIds = state.mailboxItemIds
        val currentLocation = state.messageLocation
        val currentLocationId = state.currentLocationId

        binding.textViewDetailsActionsMoveToInbox.apply {
            isVisible = state.showMoveToInboxAction
            if (currentLocation == Constants.MessageLocationType.SPAM) {
                setText(R.string.not_spam_move_to_inbox)
            }
            setOnClickListener {
                viewModel.moveToInbox(messageIds, currentLocation)
            }
        }

        binding.textViewDetailsActionsTrash.apply {
            isVisible = state.showMoveToTrashAction
            setOnClickListener {
                if (isScheduled) {
                    context.showTwoButtonInfoDialog(
                        titleStringId = R.string.scheduled_message_moved_to_trash_title,
                        messageStringId = R.string.scheduled_message_moved_to_trash_desc,
                        negativeStringId = R.string.cancel,
                        onPositiveButtonClicked = {
                            viewModel.moveToTrash(messageIds, currentLocation)
                        }
                    )
                } else
                    viewModel.moveToTrash(messageIds, currentLocation)
            }
        }

        binding.textViewDetailsActionsMoveToArchive.apply {
            isVisible = state.showMoveToArchiveAction
            setOnClickListener {
                viewModel.moveToArchive(messageIds, currentLocation)
            }
        }

        binding.textViewDetailsActionsMoveToSpam.apply {
            isVisible = state.showMoveToSpamAction
            setOnClickListener {
                viewModel.moveToSpam(messageIds, currentLocation)
            }
        }

        binding.textViewDetailsActionsDelete.apply {
            isVisible = state.showDeleteAction
            setOnClickListener {
                showDeleteConfirmationDialog(
                    context,
                    getString(R.string.delete_messages),
                    getString(R.string.confirm_destructive_action)
                ) {
                    viewModel.delete(
                        messageIds,
                        currentLocation,
                        state.actionsTarget == ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN
                    )
                }
            }
        }

        binding.textViewDetailsActionsMoveTo.setOnClickListener {
            viewModel.showLabelsManager(messageIds, currentLocation, currentLocationId, LabelType.FOLDER)
            dismiss()
        }
    }

    private fun setupMoreSectionBindings(
        binding: FragmentMessageActionSheetBinding,
        actionsTarget: ActionSheetTarget,
        messageIds: List<String>
    ) {
        with(binding) {
            val showMoreMessageOptions = actionsTarget == ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN ||
                actionsTarget == ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

            viewActionSheetSeparator.isVisible = showMoreMessageOptions
            textViewActionSheetMoreTitle.isVisible = showMoreMessageOptions

            textViewDetailsActionsPrint.apply {
                isVisible = showMoreMessageOptions
                setOnClickListener {
                    // we call it this way as it requires "special" context from the Activity
                    (activity as? MessageDetailsActivity)?.printMessage(messageIds.first())
                    dismiss()
                }
            }
            textViewDetailsActionsViewHeaders.apply {
                isVisible = showMoreMessageOptions
                setOnClickListener {
                    viewModel.showMessageHeaders(messageIds.first())
                }
            }
            textViewDetailsActionsReportPhishing.apply {
                isVisible = showMoreMessageOptions
                setOnClickListener {
                    (activity as? MessageDetailsActivity)?.showReportPhishingDialog(messageIds.first())
                    dismiss()
                }
            }
        }
    }

    /**
     * This is a bit crazy requirement but designers currently do not know what to do about it,
     * so we have to dismiss the action sheet and the Details activity at the time and go to the main list.
     * This should be improved.
     */
    private fun popBackDetailsActivity() = (activity as? MessageDetailsActivity)?.onBackPressed()

    private fun setCloseIconVisibility(shouldBeVisible: Boolean) =
        actionSheetHeader?.setCloseIconVisibility(shouldBeVisible)

    private fun processAction(sheetAction: MessageActionSheetAction) {
        Timber.v("Action received $sheetAction")
        when (sheetAction) {
            is MessageActionSheetAction.ShowLabelsManager -> showManageLabelsActionSheet(
                sheetAction.messageIds,
                sheetAction.labelActionSheetType,
                sheetAction.currentFolderLocation,
                sheetAction.currentLocationId,
                sheetAction.actionSheetTarget
            )
            is MessageActionSheetAction.ShowMessageHeaders -> showMessageHeaders(sheetAction.messageHeaders)
            is MessageActionSheetAction.ChangeStarredStatus -> {
                if (sheetAction.isSuccessful) {
                    mailboxViewModel.exitSelectionMode(sheetAction.areMailboxItemsMovedFromLocation)
                    dismiss()
                } else {
                    showCouldNotCompleteActionError()
                }
            }
            is MessageActionSheetAction.DismissActionSheet -> {
                mailboxViewModel.exitSelectionMode(sheetAction.areMailboxItemsMovedFromLocation)
                handleDismissBehavior(sheetAction.shallDismissBackingActivity)
            }
            is MessageActionSheetAction.CouldNotCompleteActionError ->
                showCouldNotCompleteActionError()
            is MessageActionSheetAction.ViewMessageInLightDarkMode -> {
                messageDetailsViewModel.reloadMessage(sheetAction.messageId)
                handleDismissBehavior(false)
            }
            else -> Timber.v("unhandled action $sheetAction")
        }
    }

    private fun handleDismissBehavior(dismissBackingActivity: Boolean) {
        dismiss()
        if (dismissBackingActivity) {
            popBackDetailsActivity()
        }
    }

    private fun showManageLabelsActionSheet(
        messageIds: List<String>,
        labelType: LabelType,
        currentFolderLocation: Int,
        currentLocationId: String,
        actionSheetTarget: ActionSheetTarget
    ) {
        LabelsActionSheet.newInstance(
            messageIds,
            currentFolderLocation,
            currentLocationId,
            labelType,
            actionSheetTarget
        )
            .show(parentFragmentManager, LabelsActionSheet::class.qualifiedName)
        dismiss()
    }

    private fun showMessageHeaders(messageHeader: String) {
        startActivity(
            AppUtil.decorInAppIntent(
                Intent(
                    context,
                    MessageViewHeadersActivity::class.java
                ).putExtra(EXTRA_VIEW_HEADERS, messageHeader)
            )
        )
        dismiss()
    }

    private fun showCouldNotCompleteActionError() {
        Toast.makeText(
            context,
            context?.getString(R.string.could_not_complete_action),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun View.isExpandedToFullscreen(): Boolean = this.top == 0

    companion object {

        private const val EXTRA_ARG_MESSAGE_IDS = "arg_message_ids"
        private const val EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID = "extra_arg_current_folder_location_id"
        private const val EXTRA_ARG_MAILBOX_LABEL_ID = "arg_mailbox_label_id"
        private const val EXTRA_ARG_TITLE = "arg_message_details_actions_title"
        private const val EXTRA_ARG_SUBTITLE = "arg_message_details_actions_sub_title"
        const val EXTRA_ARG_IS_STARRED = "arg_extra_is_stared"
        const val EXTRA_ARG_IS_SCHEDULED = "arg_extra_is_scheduled"
        private const val EXTRA_ARG_CONVERSATION_HAS_MORE_THAN_ONE_MESSAGE =
            "arg_conversation_has_more_than_one_message"
        private const val HEADER_SLIDE_THRESHOLD = 0.8f
        internal const val EXTRA_ARG_ACTION_TARGET = "extra_arg_action_sheet_actions_target"

        /**
         * Creates new action sheet instance.
         *
         * @param actionSheetTarget defines the entity this action sheet's actions will be applied to
         * @param messagesIds current message id/ or selected messages Ids
         * @param currentFolderLocationId defines current message folder location based on values from
         * [Constants.MessageLocationType] e.g. 3 = trash
         * @param title title part that will be displayed in the top header
         * @param subTitle small sub title part that will be displayed in the top header, null/empty if not needed
         * @param isStarred defines if message is currently marked as starred
         */
        fun newInstance(
            actionSheetTarget: ActionSheetTarget,
            messagesIds: List<String>,
            currentFolderLocationId: Int,
            mailboxLabelId: String,
            title: CharSequence,
            subTitle: String? = null,
            isStarred: Boolean = false,
            isScheduled: Boolean = false,
            doesConversationHaveMoreThanOneMessage: Boolean = true,
        ): MessageActionSheet {
            return MessageActionSheet().apply {
                arguments = bundleOf(
                    EXTRA_ARG_MESSAGE_IDS to messagesIds.toTypedArray(),
                    EXTRA_ARG_TITLE to title,
                    EXTRA_ARG_SUBTITLE to subTitle,
                    EXTRA_ARG_IS_STARRED to isStarred,
                    EXTRA_ARG_IS_SCHEDULED to isScheduled,
                    EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID to currentFolderLocationId,
                    EXTRA_ARG_MAILBOX_LABEL_ID to mailboxLabelId,
                    EXTRA_ARG_ACTION_TARGET to actionSheetTarget,
                    EXTRA_ARG_CONVERSATION_HAS_MORE_THAN_ONE_MESSAGE to doesConversationHaveMoreThanOneMessage
                )
            }
        }
    }
}
