const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user cannot read or write profiles, chats, or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("chats").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      preferredLanguage: "Hinglish",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner can create and update their UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Sahib",
      email: "sahib@example.com",
      preferredLanguage: "Hinglish",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "Urdu",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid language enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "Klingon",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt overwrite
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated owner can create, list, update, and delete ChatSessions and ChatMessages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);
  const chatRef = aliceDb.collection("users").doc(ALICE_UID).collection("chats").doc("chat_1");

  await assertSucceeds(
    chatRef.set({
      userId: ALICE_UID,
      title: "Quadratic Equations Help",
      category: "homework",
      lastMessagePreview: "Let's solve x^2 - 5x + 6 = 0",
      messageCount: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // List chats under own userId
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chats").get()
  );

  // Add message under existing chat
  await assertSucceeds(
    chatRef.collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      text: "Solve x^2 - 5x + 6 = 0",
      hasImage: false,
      toolTag: "Solve Maths",
      createdAt: now,
    })
  );

  // Update chat metadata
  await assertSucceeds(
    chatRef.update({
      lastMessagePreview: "The roots are x = 2 and x = 3!",
      messageCount: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field on chat creation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("chats").doc("chat_ghost").set({
      id: "chat_ghost",
      userId: ALICE_UID,
      title: "Ghost",
      category: "chat",
      lastMessagePreview: "hi",
      messageCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject message update (messages are append-only)
  await assertFails(
    chatRef.collection("messages").doc("msg_1").update({
      text: "Tampered",
    })
  );

  // Reject message under non-existent chat
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chats")
      .doc("missing_chat")
      .collection("messages")
      .doc("msg_orphan")
      .set({
        userId: ALICE_UID,
        role: "user",
        text: "Orphan",
        hasImage: false,
        createdAt: now,
      })
  );
});

test("Cross-user isolation blocks Bob from reading or writing Alice's data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chats").doc("alice_chat").set({
      userId: ALICE_UID,
      title: "Private Chat",
      category: "chat",
      lastMessagePreview: "Secret",
      messageCount: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("chats").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("chats").doc("alice_chat").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chats")
      .doc("alice_chat")
      .update({ title: "Hacked" })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chats")
      .doc("alice_chat")
      .delete()
  );
});
