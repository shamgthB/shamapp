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

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Authenticated user: can create their own profile, cannot create other profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  
  // Succeeds for own profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Smith",
      email: "alice@example.com",
      status: "Online",
      createdAt: new Date(),
    })
  );

  // Fails for Bob's profile
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Fake",
      email: "bob@example.com",
      createdAt: new Date(),
    })
  );
});

test("Channels: Alice can create channel and post message", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const channelRef = aliceDb.collection("channels").doc("tech_chat");

  await assertSucceeds(
    channelRef.set({
      channelId: "tech_chat",
      name: "Tech Chat",
      description: "Discuss technology and programming",
      createdBy: ALICE_UID,
      memberCount: 1,
      createdAt: new Date(),
    })
  );

  // Alice posts message
  await assertSucceeds(
    channelRef.collection("messages").doc("msg_1").set({
      messageId: "msg_1",
      channelId: "tech_chat",
      senderId: ALICE_UID,
      senderName: "Alice Smith",
      text: "Hello everyone in Tech Chat!",
      createdAt: new Date(),
    })
  );

  // Unauthenticated user cannot post
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("channels").doc("tech_chat").collection("messages").doc("msg_2").set({
      messageId: "msg_2",
      channelId: "tech_chat",
      senderId: "anon",
      senderName: "Anonymous",
      text: "Spam message",
      createdAt: new Date(),
    })
  );
});

test("Conversations: Alice and Bob can chat, Charlie is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const convId = "conv_alice_bob";
  const convRef = aliceDb.collection("conversations").doc(convId);

  await assertSucceeds(
    convRef.set({
      conversationId: convId,
      participantUids: [ALICE_UID, BOB_UID],
      lastMessage: "Hey Bob!",
      lastMessageSenderId: ALICE_UID,
      lastMessageAt: new Date(),
      createdAt: new Date(),
    })
  );

  // Alice sends message in conversation
  await assertSucceeds(
    convRef.collection("messages").doc("msg_dm_1").set({
      messageId: "msg_dm_1",
      conversationId: convId,
      senderId: ALICE_UID,
      senderName: "Alice Smith",
      text: "Hey Bob, how are you?",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );

  // Bob can read
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("conversations").doc(convId).get());
  await assertSucceeds(bobDb.collection("conversations").doc(convId).collection("messages").doc("msg_dm_1").get());

  // Charlie is denied
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(charlieDb.collection("conversations").doc(convId).get());
  await assertFails(charlieDb.collection("conversations").doc(convId).collection("messages").doc("msg_dm_1").get());
});
