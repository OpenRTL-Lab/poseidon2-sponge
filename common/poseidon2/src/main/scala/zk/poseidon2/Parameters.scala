/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
 * Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
 * File: common/poseidon2/src/main/scala/zk/poseidon2/Parameters.scala
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
 * Copyright applies to original project contributions only.
 * SPDX-FileCopyrightText: 2022 The Plonky3 Authors
 * Attribution: Plonky3 Poseidon2 parameters; upstream MIT notices remain applicable.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

package zk.poseidon2

// 参数固定到 Plonky3 的 Goldilocks 12元素参数；原许可证见reference。
object Parameters {
  val P = BigInt("ffffffff00000001", 16)
  val RC = Seq(
    Seq(BigInt("13dcf33aba214f46",16), BigInt("30b3b654a1da6d83",16), BigInt("1fc634ada6159b56",16), BigInt("937459964dc03466",16), BigInt("edd2ef2ca7949924",16), BigInt("ede9affde0e22f68",16), BigInt("8515b9d6bac9282d",16), BigInt("6b5c07b4e9e900d8",16), BigInt("1ec66368838c8a08",16), BigInt("9042367d80d1fbab",16), BigInt("400283564a3c3799",16), BigInt("4a00be0466bca75e",16)),
    Seq(BigInt("7913beee58e3817f",16), BigInt("f545e88532237d90",16), BigInt("22f8cb8736042005",16), BigInt("6f04990e247a2623",16), BigInt("fe22e87ba37c38cd",16), BigInt("d20e32c85ffe2815",16), BigInt("117227674048fe73",16), BigInt("4e9fb7ea98a6b145",16), BigInt("e0866c232b8af08b",16), BigInt("00bbc77916884964",16), BigInt("7031c0fb990d7116",16), BigInt("240a9e87cf35108f",16)),
    Seq(BigInt("2e6363a5a12244b3",16), BigInt("5e1c3787d1b5011c",16), BigInt("4132660e2a196e8b",16), BigInt("3a013b648d3d4327",16), BigInt("f79839f49888ea43",16), BigInt("fe85658ebafe1439",16), BigInt("b6889825a14240bd",16), BigInt("578453605541382b",16), BigInt("4508cda8f6b63ce9",16), BigInt("9c3ef35848684c91",16), BigInt("0812bde23c87178c",16), BigInt("fe49638f7f722c14",16)),
    Seq(BigInt("8e3f688ce885cbf5",16), BigInt("b8e110acf746a87d",16), BigInt("b4b2e8973a6dabef",16), BigInt("9e714c5da3d462ec",16), BigInt("6438f9033d3d0c15",16), BigInt("24312f7cf1a27199",16), BigInt("23f843bb47acbf71",16), BigInt("9183f11a34be9f01",16), BigInt("839062fbb9d45dbf",16), BigInt("24b56e7e6c2e43fa",16), BigInt("e1683da61c962a72",16), BigInt("a95c63971a19bfa7",16)),
    Seq(BigInt("4adf842aa75d4316",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("f8fbb871aa4ab4eb",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("68e85b6eb2dd6aeb",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("07a0b06b2d270380",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("d94e0228bd282de4",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("8bdd91d3250c5278",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("209c68b88bba778f",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("b5e18cdab77f3877",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("b296a3e808da93fa",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("8370ecbda11a327e",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("3f9075283775dad8",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("b78095bb23c6aa84",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("3f36b9fe72ad4e5f",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("69bc96780b10b553",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("3f1d341f2eb7b881",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("4e939e9815838818",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("da366b3ae2a31604",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("bc89db1e7287d509",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("6102f411f9ef5659",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("58725c5e7ac1f0ab",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("0df5856c798883e7",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("f7bb62a8da4c961b",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16), BigInt("0",16)),
    Seq(BigInt("c68be7c94882a24d",16), BigInt("af996d5d5cdaedd9",16), BigInt("9717f025e7daf6a5",16), BigInt("6436679e6e7216f4",16), BigInt("8a223d99047af267",16), BigInt("bb512e35a133ba9a",16), BigInt("fbbf44097671aa03",16), BigInt("f04058ebf6811e61",16), BigInt("5cca84703fac7ffb",16), BigInt("9b55c7945de6469f",16), BigInt("8e05bf09808e934f",16), BigInt("2ea900de876307d7",16)),
    Seq(BigInt("7748fff2b38dfb89",16), BigInt("6b99a676dd3b5d81",16), BigInt("ac4bb7c627cf7c13",16), BigInt("adb6ebe5e9e2f5ba",16), BigInt("2d33378cafa24ae3",16), BigInt("1e5b73807543f8c2",16), BigInt("09208814bfebb10f",16), BigInt("782e64b6bb5b93dd",16), BigInt("add5a48eac90b50f",16), BigInt("add4c54c736ea4b1",16), BigInt("d58dbb86ed817fd8",16), BigInt("6d5ed1a533f34ddd",16)),
    Seq(BigInt("28686aa3e36b7cb9",16), BigInt("591abd3476689f36",16), BigInt("047d766678f13875",16), BigInt("a2a11112625f5b49",16), BigInt("21fd10a3f8304958",16), BigInt("f9b40711443b0280",16), BigInt("d2697eb8b2bde88e",16), BigInt("3493790b51731b3f",16), BigInt("11caf9dd73764023",16), BigInt("7acfb8f72878164e",16), BigInt("744ec4db23cefc26",16), BigInt("1e00e58f422c6340",16)),
    Seq(BigInt("21dd28d906a62dda",16), BigInt("f32a46ab5f465b5f",16), BigInt("bfce13201f3f7e6b",16), BigInt("f30d2e7adb5304e2",16), BigInt("ecdf4ee4abad48e9",16), BigInt("f94e82182d395019",16), BigInt("4ee52e3744d887c5",16), BigInt("a1341c7cac0083b2",16), BigInt("2302fb26c30c834a",16), BigInt("aea3c587273bf7d3",16), BigInt("f798e24961823ec7",16), BigInt("962deba3e9a2cd94",16))
  )
}
