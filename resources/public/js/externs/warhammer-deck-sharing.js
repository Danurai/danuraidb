"use strict";

/*
Format of deck codes version 1:
<byte 0xFF>, <byte version>, [<byte count><32 bit id>, <byte count><32 bit id>, ...]
 */

const MAX_SUPPORTED_VERSION = 1
const BYTES_PER_CARD = 5
const IN_BROWSER = typeof window !== "undefined"

function fromBase64 (b64) {
  return IN_BROWSER ? window.atob(b64) : Buffer.from(b64, "base64").toString("latin1")
}

function toUrlSafeBase64 (str) {
  let b64 = IN_BROWSER ? window.btoa(str) : Buffer.from(str, "latin1").toString("base64")
  return b64.replace(/\//g, "_").replace(/\+/g, "-")
}

function getVersionAndData (raw) {
  const buffer = new ArrayBuffer(raw.length)
  const view = new DataView(buffer)
  for (let i = 0; i < raw.length; i++) {
    view.setUint8(i, raw.charCodeAt(i))
  }
  let version = 0
  const magicNumber = view.getUint8(0)
  if (magicNumber === 0xFF) {
    version = view.getUint8(1)
  }
  return {
    version,
    data: new DataView(buffer, version === 0 ? 0 : 2)
  }
}

function getCards (data) {
  const cards = []
  for (let i = 0; i < data.byteLength; i += BYTES_PER_CARD) {
    cards.push({
      count: data.getUint8(i),
      id: data.getInt32(i + 1, true)
    })
  }
  return cards
}

function parseQueryStringDeckCode (qsDeckCode) {
  return qsDeckCode.replace(/_/g, "/").replace(/[- ]/g, "+")
}


/**
 * Given a deck code, returns an object with the deck code version, and an array of objects like {id: <card id>, count: <card count>}
 * @param deckCode
 * @return {{version: number, cards: [{id: number, count: number}]}}
 */
function parse (deckCode) {
  const raw = fromBase64(parseQueryStringDeckCode(deckCode))
  const {version, data} = getVersionAndData(raw)
  if (version > MAX_SUPPORTED_VERSION) {
    throw new Error("Unsupported deck sharing version: " + version)
  }
  return {
    version,
    cards: getCards(data)
  }
}

/**
 * Generate a deck code from a list of cards and their counts
 * @param {array} cards  Each element of the array should be an object containing the card id and count, e.g. {id: 123456, count: 2} (other keys are ignored)
 * @return {string}      Deck code base64 encoded which can be imported into the game
 */
function generate (cards) {
  // Sort the cards for consistency, so the same deck in a different order produces the same deck code
  const sortedCards = cards.sort((a, b) => b.id > a.id ? -1 : 1)
  const buffer = new ArrayBuffer(2 + sortedCards.length * BYTES_PER_CARD)
  const view = new DataView(buffer)
  // Set magic number and version byte
  view.setUint8(0, 0xFF)
  view.setUint8(1, MAX_SUPPORTED_VERSION)
  // Set cards
  sortedCards.forEach(({id, count}, i) => {
    if (!id) throw new Error("Missing card id at index " + i)
    if (!count) throw new Error("Missing or zero card count at index " + i)
    const offset = 2 + (i * BYTES_PER_CARD)
    view.setUint8(offset, count)
    view.setUint32(offset + 1, id, true)
  })
  // Convert to a base64 string
  const str = String.fromCharCode.apply(null, new Uint8Array(buffer))
  return toUrlSafeBase64(str)
}

//module.exports = {MAX_SUPPORTED_VERSION, parse, generate}