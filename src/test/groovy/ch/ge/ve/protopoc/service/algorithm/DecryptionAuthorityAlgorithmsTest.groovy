package ch.ge.ve.protopoc.service.algorithm

import ch.ge.ve.protopoc.service.model.*
import ch.ge.ve.protopoc.service.support.RandomGenerator
import spock.lang.Specification

import static ch.ge.ve.protopoc.service.support.BigIntegers.*
import static java.math.BigInteger.ONE
import static java.math.BigInteger.ZERO

/**
 * Tests for the algorithms performed during the decryption phase
 */
class DecryptionAuthorityAlgorithmsTest extends Specification {
    // Primary Mocks
    PublicParameters publicParameters = Mock()
    GeneralAlgorithms generalAlgorithms = Mock()
    RandomGenerator randomGenerator = Mock()

    // Secondary Mocks
    EncryptionGroup encryptionGroup = Mock()

    // Class under test
    DecryptionAuthorityAlgorithms decryptionAuthorityAlgorithms

    void setup() {
        publicParameters.encryptionGroup >> encryptionGroup
        publicParameters.s >> 2

        encryptionGroup.p >> ELEVEN
        encryptionGroup.q >> FIVE // G_q = (1, 3, 4, 5, 9)
        encryptionGroup.g >> THREE
        encryptionGroup.h >> FOUR

        decryptionAuthorityAlgorithms = new DecryptionAuthorityAlgorithms(publicParameters, generalAlgorithms, randomGenerator)

    }

    def "checkShuffleProofs should check the shuffles performed by the other authorities"() {
        given: "a series of encryptions"
        def e_0 = [
                new Encryption(FIVE, ONE),
                new Encryption(THREE, FOUR),
                new Encryption(FIVE, NINE)
        ]
        def e_1 = [
                new Encryption(ONE, FIVE),
                new Encryption(FOUR, THREE),
                new Encryption(ONE, FOUR)
        ]
        def e_2 = [
                new Encryption(NINE, FIVE),
                new Encryption(ONE, NINE),
                new Encryption(FOUR, THREE)
        ]
        def bold_E = [e_1, e_2]

        and: "a public key"
        def pk = new EncryptionPublicKey(THREE, encryptionGroup)

        and: "a valid shuffle proof"
        def t = new ShuffleProof.T(THREE, NINE, FIVE, [THREE, FOUR], [FOUR, FOUR, THREE])
        def s = new ShuffleProof.S(ZERO, TWO, FOUR, ZERO, [THREE, FOUR, ZERO], [FOUR, THREE, THREE])
        def bold_c = [NINE, THREE, THREE]
        def bold_c_circ = [ONE, ONE, THREE]
        def pi = new ShuffleProof(t, s, bold_c, bold_c_circ)

        def bold_pi = [pi, null]

        and: "some mocked collaborators"
        generalAlgorithms.getGenerators(3) >> [FOUR, THREE, FIVE]
        generalAlgorithms.getChallenges(3, [e_0, e_1, [NINE, THREE, THREE]] as List[], FIVE) >>
                [TWO, FOUR, THREE]
        generalAlgorithms.getNIZKPChallenge(_, _, _) >> FOUR

        and: "the expected preconditions"
        generalAlgorithms.isMember(ONE) >> true
        generalAlgorithms.isMember(THREE) >> true
        generalAlgorithms.isMember(FOUR) >> true
        generalAlgorithms.isMember(FIVE) >> true
        generalAlgorithms.isMember(NINE) >> true
        generalAlgorithms.isInZ_q(_ as BigInteger) >> { BigInteger it -> 0 <= it && it < encryptionGroup.q }

        and: "an authority index"
        int j = 1

        expect:
        decryptionAuthorityAlgorithms.checkShuffleProofs(bold_pi, e_0, bold_E, pk, j) // == true implied
    }

    def "checkShuffleProofs should fail given an invalid proof"() {
        given: "a series of encryptions"
        def e_0 = [
                new Encryption(FIVE, ONE),
                new Encryption(THREE, FOUR),
                new Encryption(FIVE, NINE)
        ]
        def e_1 = [
                new Encryption(ONE, FIVE),
                new Encryption(FOUR, THREE),
                new Encryption(ONE, FOUR)
        ]
        def e_2 = [
                new Encryption(NINE, FIVE),
                new Encryption(ONE, NINE),
                new Encryption(FOUR, THREE)
        ]
        def bold_E = [e_1, e_2]

        and: "a public key"
        def pk = new EncryptionPublicKey(THREE, encryptionGroup)

        and: "an invalid shuffle proof"
        def t = new ShuffleProof.T(FOUR /* invalid data */, NINE, FIVE, [THREE, FOUR], [FOUR, FOUR, THREE])
        def s = new ShuffleProof.S(ZERO, TWO, FOUR, ZERO, [THREE, FOUR, ZERO], [FOUR, THREE, THREE])
        def bold_c = [NINE, THREE, THREE]
        def bold_c_circ = [ONE, ONE, THREE]
        def pi = new ShuffleProof(t, s, bold_c, bold_c_circ)

        def bold_pi = [pi, null]

        and: "some mocked collaborators"
        generalAlgorithms.getGenerators(3) >> [FOUR, THREE, FIVE]
        generalAlgorithms.getChallenges(3, [e_0, e_1, [NINE, THREE, THREE]] as List[], FIVE) >>
                [TWO, FOUR, THREE]
        generalAlgorithms.getNIZKPChallenge(_, _, _) >> FOUR

        and: "the expected preconditions"
        generalAlgorithms.isMember(ONE) >> true
        generalAlgorithms.isMember(THREE) >> true
        generalAlgorithms.isMember(FOUR) >> true
        generalAlgorithms.isMember(FIVE) >> true
        generalAlgorithms.isMember(NINE) >> true
        generalAlgorithms.isInZ_q(_ as BigInteger) >> { BigInteger it -> 0 <= it && it < encryptionGroup.q }

        and: "an authority index"
        int j = 1

        expect:
        !decryptionAuthorityAlgorithms.checkShuffleProofs(bold_pi, e_0, bold_E, pk, j)
    }

    def "checkShuffleProof should correctly validate a shuffle proof"() {
        given: "some input"
        def bold_e = [
                new Encryption(FIVE, ONE),
                new Encryption(THREE, FOUR),
                new Encryption(FIVE, NINE)
        ]
        def bold_e_prime = [
                new Encryption(ONE, FIVE),
                new Encryption(FOUR, THREE),
                new Encryption(ONE, FOUR)
        ]
        def pk = new EncryptionPublicKey(THREE, encryptionGroup)
        def t = new ShuffleProof.T(THREE, NINE, FIVE, [THREE, FOUR], [FOUR, FOUR, THREE])
        def s = new ShuffleProof.S(ZERO, TWO, FOUR, ZERO, [THREE, FOUR, ZERO], [FOUR, THREE, THREE])
        def bold_c = [NINE, THREE, THREE]
        def bold_c_circ = [ONE, ONE, THREE]
        def pi = new ShuffleProof(t, s, bold_c, bold_c_circ)

        and: "some mocked collaborators"
        generalAlgorithms.getGenerators(3) >> [FOUR, THREE, FIVE]
        generalAlgorithms.getChallenges(3, [bold_e, bold_e_prime, [NINE, THREE, THREE]] as List[], FIVE) >>
                [TWO, FOUR, THREE]
        generalAlgorithms.getNIZKPChallenge(_, _, _) >> FOUR

        and: "the expected preconditions"
        generalAlgorithms.isMember(ONE) >> true
        generalAlgorithms.isMember(THREE) >> true
        generalAlgorithms.isMember(FOUR) >> true
        generalAlgorithms.isMember(FIVE) >> true
        generalAlgorithms.isMember(NINE) >> true
        generalAlgorithms.isInZ_q(_ as BigInteger) >> { BigInteger it -> 0 <= it && it < encryptionGroup.q }

        expect:
        decryptionAuthorityAlgorithms.checkShuffleProof(pi, bold_e, bold_e_prime, pk) // == true implied
    }

    def "getPartialDecryptions should perform partial decryptions on provided encryptions"() {
        given:
        def bold_e = [
                new Encryption(ONE, FIVE),
                new Encryption(NINE, THREE),
                new Encryption(FOUR, FOUR),
                new Encryption(FIVE, NINE)
        ]
        def sk_j = THREE

        and: "the expected preconditions"
        generalAlgorithms.isMember(ONE) >> true
        generalAlgorithms.isMember(THREE) >> true
        generalAlgorithms.isMember(FOUR) >> true
        generalAlgorithms.isMember(FIVE) >> true
        generalAlgorithms.isMember(NINE) >> true

        expect:
        decryptionAuthorityAlgorithms.getPartialDecryptions(bold_e, sk_j) == [FOUR, FIVE, NINE, THREE]
    }

    def "genDecryptionProof should generate a valid decryption proof"() {
        given:
        def sk_j = THREE
        def pk_j = FIVE
        def bold_e = [
                new Encryption(ONE, FIVE),
                new Encryption(NINE, THREE),
                new Encryption(FOUR, FOUR),
                new Encryption(FIVE, NINE)
        ]
        def bold_b_prime = [FOUR, FIVE, NINE, THREE]
        randomGenerator.randomInZq(FIVE) >> TWO
        generalAlgorithms.getNIZKPChallenge(*_) >> ONE

        expect:
        decryptionAuthorityAlgorithms.genDecryptionProof(sk_j, pk_j, bold_e, bold_b_prime) ==
                new DecryptionProof([NINE, THREE, NINE, FIVE, FOUR], ZERO)
    }
}
