const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Username check: unauthenticated or authenticated can check specific username", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  // Document get on valid username succeeds (even if doc doesn't exist)
  await assertSucceeds(unauthDb.collection("usernames").doc("alice_cool").get());

  // Listing all usernames is strictly denied to prevent user enumeration
  await assertFails(unauthDb.collection("usernames").get());
});

test("Username claim: authenticated user can claim unique username for themselves", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Alice claims "alice_cool"
  await assertSucceeds(
    aliceDb.collection("usernames").doc("alice_cool").set({
      username: "alice_cool",
      userId: ALICE_UID,
      createdAt: new Date(),
    })
  );

  // Bob cannot claim Alice's username or claim with another's UID
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("usernames").doc("alice_cool").set({
      username: "alice_cool",
      userId: BOB_UID,
      createdAt: new Date(),
    })
  );
});

test("Privacy rule: listing all users is denied, but individual user get is allowed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Create Alice's profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice",
      displayName: "Alice",
      email: "alice@example.com",
      createdAt: new Date(),
    })
  );

  // Listing the entire users collection is strictly denied
  await assertFails(aliceDb.collection("users").get());

  // Individual user lookup by ID is allowed
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Chat privacy: only conversation participants can read conversation and messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const convId = "conv_alice_bob";

  // Alice creates conversation with Bob
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).set({
      conversationId: convId,
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );

  // Alice sends message
  const msgId = "msg_1";
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).collection("messages").doc(msgId).set({
      messageId: msgId,
      conversationId: convId,
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Hello Bob! Secret chat.",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );

  // Bob (participant) can read message
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("conversations").doc(convId).collection("messages").doc(msgId).get());

  // Charlie (stranger) CANNOT read conversation or messages
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(charlieDb.collection("conversations").doc(convId).get());
  await assertFails(charlieDb.collection("conversations").doc(convId).collection("messages").doc(msgId).get());
});
