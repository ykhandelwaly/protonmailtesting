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
package ch.protonmail.android.api.models.messages.send;

import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.api.models.messages.receive.AttachmentFactory;
import ch.protonmail.android.api.models.messages.receive.MessageFactory;
import ch.protonmail.android.api.models.messages.receive.MessageLocationResolver;
import ch.protonmail.android.api.models.messages.receive.MessageSenderFactory;
import ch.protonmail.android.api.models.messages.receive.ServerMessage;
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.details.data.MessageFlagsToEncryptionMapper;

public class MessageSendResponse extends ResponseBody {
    private ServerMessage Sent;
    private MessageParent Parent;

    public Message getSent() {
        final AttachmentFactory attachmentFactory = new AttachmentFactory();
        MessageSenderFactory messageSenderFactory = new MessageSenderFactory();
        final MessageLocationResolver messageLocationResolver = new MessageLocationResolver(null);
        final MessageFlagsToEncryptionMapper messageFlagsMapper = new MessageFlagsToEncryptionMapper();
        final MessageFactory messageFactory = new MessageFactory(
                attachmentFactory,
                messageSenderFactory,
                messageLocationResolver,
                messageFlagsMapper
        );
        return messageFactory.createMessage(Sent);
    }

    public MessageParent getParent(){
        return Parent;
    }

    public class MessageParent{
        private String ID;
        private int IsReplied;
        private int IsRepliedAll;
        private int IsForwarded;

        public String getID() {
            return ID;
        }

        public int getIsReplied() {
            return IsReplied;
        }

        public int getIsRepliedAll() {
            return IsRepliedAll;
        }

        public int getIsForwarded() {
            return IsForwarded;
        }


    }
}
