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

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read expenses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("expenses").get());
});

test("Authenticated user: cannot read another user's expenses", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("expenses").doc("exp_1").set({
      userId: BOB_UID,
      amount: 25.5,
      currency: "PEN",
      category: "comida",
      description: "Almuerzo",
      paymentMethod: "yape",
      date: "2026-10-27",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("expenses").doc("exp_1").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("expenses").get());
});

test("Authenticated user: can create and read their own valid expense", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_alice").set({
      userId: ALICE_UID,
      amount: 15.0,
      currency: "PEN",
      category: "comida",
      description: "Menú con chicha",
      paymentMethod: "efectivo",
      rawInput: "15 menú con chicha",
      date: "2026-10-27",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_alice").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").get()
  );
});

test("Authenticated user: cannot spoof userId on expense creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_spoof").set({
      userId: BOB_UID, // Spoofed UID
      amount: 15.0,
      currency: "PEN",
      category: "comida",
      description: "Fake expense",
      paymentMethod: "efectivo",
      date: "2026-10-27",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: rejects negative or invalid amount", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_bad").set({
      userId: ALICE_UID,
      amount: -10,
      currency: "PEN",
      category: "comida",
      description: "Bad amount",
      paymentMethod: "efectivo",
      date: "2026-10-27",
      createdAt: new Date(),
    })
  );
});
