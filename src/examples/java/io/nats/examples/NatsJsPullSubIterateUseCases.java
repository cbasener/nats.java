// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.examples;

import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;

import static io.nats.examples.ExampleUtils.sleep;
import static io.nats.examples.NatsJsUtils.*;

/**
 * This example will demonstrate miscellaneous uses cases of a pull subscription of:
 * iterate pull: <code>iterate(int batchSize, Duration maxWait)</code>,
 * no manual handling of null or status.
 *
 * Usage: java NatsJsPullSubIterateUseCases [-s server] [-strm stream] [-sub subject] [-dur durable]
 *   Use tls:// or opentls:// to require tls, via the Default SSLContext
 *   Set the environment variable NATS_NKEY to use challenge response authentication by setting a file containing your private key.
 *   Set the environment variable NATS_CREDS to use JWT/NKey authentication by setting a file containing your user creds.
 *   Use the URL for user/pass/token authentication.
 */
public class NatsJsPullSubIterateUseCases {

    public static void main(String[] args) {
        ExampleArgs exArgs = ExampleArgs.builder()
                .defaultStream("iterate-uc-ack-stream")
                .defaultSubject("iterate-uc-ack-subject")
                .defaultDurable("iterate-uc-ack-durable")
                .uniqueify() // uncomment to be able to re-run without re-starting server
                .build(args);
        
        try (Connection nc = Nats.connect(ExampleUtils.createExampleOptions(exArgs.server))) {

            createStreamThrowWhenExists(nc, exArgs.stream, exArgs.subject);

            // Create our JetStream context to receive JetStream messages.
            JetStream js = nc.jetStream();

            // Build our consumer configuration and subscription options.
            // make sure the ack wait is sufficient to handle the reading and processing of the batch.
            // Durable is REQUIRED for pull based subscriptions
            ConsumerConfiguration cc = ConsumerConfiguration.builder()
                    .ackWait(Duration.ofMillis(2500))
                    .build();

            PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
                    .durable(exArgs.durable) // required
                    .configuration(cc)
                    .build();

            // 0.1 Initialize. subscription
            // 0.2 Flush outgoing communication with/to the server, useful when app is both publishing and subscribing.
            System.out.println("\n----------\n0. Initialize the subscription and pull.");
            JetStreamSubscription sub = js.subscribe(exArgs.subject, pullOptions);
            nc.flush(Duration.ofSeconds(1));

            // 1. iterate, but there are no messages yet.
            // -  Read the messages, get them all (0)
            System.out.println("----------\n1. There are no messages yet");
            Iterator<Message> iterator = sub.iterate(10, Duration.ofSeconds(3));
            List<Message> messages = report(iterator);
            messages.forEach(Message::ack);
            System.out.println("We should have received 0 total messages, we received: " + messages.size());

            // 2. Publish 10 messages
            // -  iterate messages, get 10
            System.out.println("----------\n2. Publish 10 which satisfies the batch");
            publish(js, exArgs.subject, "A", 10);
            iterator = sub.iterate(10, Duration.ofSeconds(3));
            messages = report(iterator);
            messages.forEach(Message::ack);
            System.out.println("We should have received 10 total messages, we received: " + messages.size());

            // 3. Publish 20 messages
            // -  iterate messages, only get 10
            System.out.println("----------\n3. Publish 20 which is larger than the batch size.");
            publish(js, exArgs.subject, "B", 20);
            iterator = sub.iterate(10, Duration.ofSeconds(3));
            messages = report(iterator);
            messages.forEach(Message::ack);
            System.out.println("We should have received 10 total messages, we received: " + messages.size());

            // 4. There are still messages left from the last
            // -  iterate messages, get 10
            System.out.println("----------\n4. Get the rest of the publish.");
            iterator = sub.iterate(10, Duration.ofSeconds(3));
            messages = report(iterator);
            messages.forEach(Message::ack);
            System.out.println("We should have received 10 total messages, we received: " + messages.size());

            // 5. Publish 5 messages
            // -  iterate messages, get 5
            // -  Since there are less than batch size we only get what the server has.
            System.out.println("----------\n5. Publish 5 which is less than batch size.");
            publish(js, exArgs.subject, "C", 5);
            iterator = sub.iterate(10, Duration.ofSeconds(3));
            messages = report(iterator);
            messages.forEach(Message::ack);
            System.out.println("We should have received 5 total messages, we received: " + messages.size());

            // 6. Publish 15 messages
            // -  iterate messages, only get 10
            System.out.println("----------\n6. Publish 15 which is more than the batch size.");
            publish(js, exArgs.subject, "D", 15);
            iterator = sub.iterate(10, Duration.ofSeconds(3));
            messages = report(iterator);
            messages.forEach(Message::ack);
            System.out.println("We should have received 10 total messages, we received: " + messages.size());

            // 7. There are 5 messages left
            // -  iterate messages, only get 5
            System.out.println("----------\n7. There are 5 messages left.");
            iterator = sub.iterate(10, Duration.ofSeconds(3));
            messages = report(iterator);
            messages.forEach(Message::ack);
            System.out.println("We should have received 5 messages, we received: " + messages.size());

            // 8. Read but don't ack.
            // -  iterate messages, get 10, but either take too long to ack them or don't ack them
            System.out.println("----------\n8. iterate but don't ack.");
            publish(js, exArgs.subject, "E", 10);
            iterator = sub.iterate(10, Duration.ofSeconds(3));
            messages = report(iterator);
            System.out.println("We should have received 10 message, we received: " + messages.size());
            sleep(3000); // longer than the ackWait

            // 9. iterate messages,
            // -  get the 10 messages we didn't ack
            System.out.println("----------\n9. iterate, get the messages we did not ack.");
            iterator = sub.iterate(10, Duration.ofSeconds(3));
            messages = report(iterator);
            messages.forEach(Message::ack);
            System.out.println("We should have received 10 message, we received: " + messages.size());

            System.out.println("----------\n");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
