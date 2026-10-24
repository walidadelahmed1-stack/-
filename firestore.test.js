const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
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

test("Unauthenticated user: cannot read user progress", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("progress").doc("data").get());
});

test("Authenticated user (Alice): can write and read her own progress", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("data");

  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      starsJson: "{}",
      scoresJson: "{}",
      solvedPuzzles: ["1-1", "1-2"],
      storiesSeen: [1],
      highestChapter: 1,
      totalStars: 6,
      totalScore: 1200,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(docRef.get());
});

test("Authenticated user (Bob): cannot read or write Alice progress", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("progress").doc("data").set({
      userId: ALICE_UID,
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDocRef = bobDb.collection("users").doc(ALICE_UID).collection("progress").doc("data");

  await assertFails(aliceDocRef.get());
  await assertFails(
    aliceDocRef.set({
      userId: BOB_UID,
      updatedAt: new Date(),
    })
  );
});

test("Malicious write: cannot spoof another userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("data");

  await assertFails(
    docRef.set({
      userId: BOB_UID, // Spoofed!
      updatedAt: new Date(),
    })
  );
});
