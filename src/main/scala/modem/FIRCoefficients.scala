package modem

object FIRCoefficients {

  /* The stable modulation index for BLE is between 0.45 and 0.55. So our target value is h = 0.5.
      ∆f, the peak frequency deviation is defined as:
          ∆f = h * 0.5 * 1/Ts = 250kHz
      Thus, our frequency for 0 is f_IF - ∆f = 2MHz - 250kHz = 1.75MHz
            Our frequency for 1 is f_IF + ∆f = 2MHz + 250kHz = 2.25MHz
   */

  /* Design Parameters:
      Order: 30, Fs: 20MHz, Fs1: 1.25MHz, Fp1: 1.5MHz, Fp2: 2MHz, Fs2: 2.25MHz
  */
  object GFSKRX_Bandpass_F0 {
    val width = 10
    val binaryPoint = 9
    val coefficients = Seq[Double](elems =
      -0.094074222317479180843768915565306087956,
      0.057321183857839429209857229352564900182,
      0.060099581115815675635971615520247723907,
      0.064165026778876546598162633472384186462,
      0.060841639985413559221782975328096654266,
      0.045610189325746175459386222428292967379,
      0.019160504990253478252437702167298994027,
      -0.012819275833549349102868930572185490746,
      -0.04151137603573930301426031519440584816 ,
      -0.058021133666310481524774900208285544068,
      -0.056623792456428623243169795387075282633,
      -0.037042476177981793761251338992224191315,
      -0.004867869118329665005517892240050059627,
      0.0300818720741499838478016926046620938,
      0.056975533836494250683468720808377838694,
      0.066792092508099643044516824375023134053,
      0.056975533836494250683468720808377838694,
      0.0300818720741499838478016926046620938,
      -0.004867869118329665005517892240050059627,
      -0.037042476177981793761251338992224191315,
      -0.056623792456428623243169795387075282633,
      -0.058021133666310481524774900208285544068,
      -0.04151137603573930301426031519440584816,
      -0.012819275833549349102868930572185490746,
      0.019160504990253478252437702167298994027,
      0.045610189325746175459386222428292967379,
      0.060841639985413559221782975328096654266,
      0.064165026778876546598162633472384186462,
      0.060099581115815675635971615520247723907,
      0.057321183857839429209857229352564900182,
      -0.094074222317479180843768915565306087956)
  }

  /* Design Parameters:
      Order: 30, Fs: 20MHz, Fs1: 1.75MHz, Fp1: 1.95MHz, Fp2: 2.5MHz, Fs2: 2.75MHz
  */
  object GFSKRX_Bandpass_F1 {
    val width = 10
    val binaryPoint = 9
    val coefficients = Seq[Double](elems =
      0.036257583826635152168815778850330389105,
      -0.134069616956033116350610612244054209441,
      -0.068003533618812603278591666366992285475,
      -0.034884194053076890418285671557896421291,
      0.00270931594692610079946892831515015132 ,
      0.039743684855618119367015594889380736277,
      0.059522120529467391347733240536399534903,
      0.050716754630698773764763132021471392363,
      0.015369842275194741873511894425519130891,
      -0.030627747938741886613023268637334695086,
      -0.06433270742405250430806518124882131815 ,
      -0.067839609048035001692689149876969167963,
      -0.037988451633860741141202055359826772474,
      0.011529031020552891334540746015591139439,
      0.056465830098502360623324847210824373178,
      0.07442748684082461929634177977277431637 ,
      0.056465830098502360623324847210824373178,
      0.011529031020552891334540746015591139439,
      -0.037988451633860741141202055359826772474,
      -0.067839609048035001692689149876969167963,
      -0.06433270742405250430806518124882131815 ,
      -0.030627747938741886613023268637334695086,
      0.015369842275194741873511894425519130891,
      0.050716754630698773764763132021471392363,
      0.059522120529467391347733240536399534903,
      0.039743684855618119367015594889380736277,
      0.00270931594692610079946892831515015132 ,
      -0.034884194053076890418285671557896421291,
      -0.068003533618812603278591666366992285475,
      -0.134069616956033116350610612244054209441,
      0.036257583826635152168815778850330389105)
  }


  /* Design Parameters:
      Order: 30, Fs: 20MHz, Fs1: 1.75MHz, Fp1: 2MHz, Fp2: 2.5MHz, Fs2: 2.75MHz
  */
  object GFSKRX_Bandpass_F1_ALT {
    val width = 10
    val binaryPoint = 9
    val coefficients = Seq[Double](elems =
      0.032731488958079778262000303357126540504,
      -0.121031180897020987363710275985795306042,
      -0.061390105871297492856619726353528676555,
      -0.031491663038534067897700197136146016419,
      0.002445831620352381750166381735311915691,
      0.035878562350573309225332252481166506186,
      0.053733520694243239179677118499967036769,
      0.045784487519129189736499796481439261697,
      0.013875106105343926188311343139503151178,
      -0.027649161572960111871166688501944008749,
      -0.058076272063856675120785411081669735722,
      -0.06124212316776096731718581622772035189 ,
      -0.034294027730409147591839058577534160577,
      0.010407818495323243462857831787005125079,
      0.050974458287543822354592748524737544358,
      0.067189321697671583466515699001320172101,
      0.050974458287543822354592748524737544358,
      0.010407818495323243462857831787005125079,
      -0.034294027730409147591839058577534160577,
      -0.06124212316776096731718581622772035189 ,
      -0.058076272063856675120785411081669735722,
      -0.027649161572960111871166688501944008749,
      0.013875106105343926188311343139503151178,
      0.045784487519129189736499796481439261697,
      0.053733520694243239179677118499967036769,
      0.035878562350573309225332252481166506186,
      0.002445831620352381750166381735311915691,
      -0.031491663038534067897700197136146016419,
      -0.061390105871297492856619726353528676555,
      -0.121031180897020987363710275985795306042,
      0.032731488958079778262000303357126540504)
  }



  /* Design Parameters:
      Order: 20, Fs: 20MHz, Fp: 1MHz, Fs: 2MHz
  */
  object GFSKRX_Envelope_Detector {
    val width = 10
    val binaryPoint = 9
    var coefficients = Seq[Double](elems =
      -0.038983361544761306605177964001995860599,
      -0.018680941683653921187024948835642135236,
      -0.014614362321783348763748477949775406159,
      -0.002882180148878617145097313922974535672,
      0.016844935768310469181852084830097737722,
      0.043289077443031419301089357531964196824,
      0.073632342324690930324138093965302687138,
      0.103910688309522408756535583052027504891,
      0.129789498980929396410033405118156224489,
      0.147170360958872703527333669626386836171,
      0.153286373831160949166019236145075410604,
      0.147170360958872703527333669626386836171,
      0.129789498980929396410033405118156224489,
      0.103910688309522408756535583052027504891,
      0.073632342324690930324138093965302687138,
      0.043289077443031419301089357531964196824,
      0.016844935768310469181852084830097737722,
      -0.002882180148878617145097313922974535672,
      -0.014614362321783348763748477949775406159,
      -0.018680941683653921187024948835642135236,
      -0.038983361544761306605177964001995860599)
  }

  object GFSKRX_Hilbert_Filter {
    val width = 10
    val binaryPoint = 9
    val coefficients = Seq[Double](elems =
      0.0,
      0.0,
      0.0,
      0.002,
      0.0,
      0.008,
      0.0,
      0.026,
      0.0,
      0.068,
      0.0,
      0.17,
      0.0,
      0.6212,
      0.0,
      -0.6212,
      0.0,
      -0.17,
      0.0,
      -0.068,
      0.0,
      -0.026,
      0.0,
      -0.008,
      0.0,
      -0.002,
      0.0,
      0.0,
      0.0)
  }


  val gaussianWeights = Seq[Double](0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.015625, 0.0625, 0.15625, 0.328125,
    0.59375, 0.9375, 1.265625, 1.484375, 1.484375, 1.265625, 0.9375, 0.59375, 0.328125, 0.15625, 0.0625, 0.015625)
    // Removed last 7 coeffs: 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
}
