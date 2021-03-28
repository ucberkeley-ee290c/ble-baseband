import matplotlib.pyplot as plt
import numpy as np
from fixedpoint import FixedPoint
from scipy.signal import butter, lfilter
from scipy import signal
from numpy import pi
from scipy.fft import fft, fftfreq, fftshift
import fixedpoint
import math

# Constants

MHz = lambda f: f * 1000000
GHz = lambda f: f * 1000000

channel_index = 0
F_RF = MHz(2402 + 2 * channel_index) # 2.402 GHz
F_IF = MHz(2.5)  # 2.5 MHz
F_LO = F_RF - F_IF # LO frequency is RF frequency - Intermediate Frequency
F_IM = F_LO - F_IF # Image is on the other side of the LO
analog_F_sample = (F_LO * 2 + F_IF) * 2
ADC_sample_rate = MHz(20)
t_interval = 0.00001

HB_coeff = [-0.0000,    0.0001,    0.0000,   -0.0009,   -0.0000,    0.0040,    0.0000,   -0.0128,   -0.0000, 0.0340,    0.0000,   -0.0850,   -0.0000,    0.3106,    0.5000,    0.3106,   -0.0000,   -0.0850, 0.0000,    0.0340,  -0.0000,   -0.0128,    0.0000,    0.0040,   -0.0000,   -0.0009,    0.0000, 0.0001,   -0.0000]
""" Method of obtaining Hilbert Transform FIR coefficients
https://www.wirelessinnovation.org/assets/Proceedings/2011/2011-1b-carrick.pdf
"""

HB_coeff = [2 * np.sin(i * pi / 2) * HB_coeff[i] for i in range(0, len(HB_coeff))]
#print(HB_coeff)

#HB_coeff = [0.0, 0.0, 0.0, 0.002, 0.0, 0.008, 0.0, 0.026, 0.0, 0.068, 0.0, 0.17, 0.0, 0.6212, 0.0, -0.6212, 0.0, -0.17, 0.0, -0.068, 0.0, -0.026, 0.0, -0.008, 0.0, -0.002, 0.0, 0.0, 0.0]

HB_coeff = [FixedPoint(c, True, 1, 11, str_base=2) for c in HB_coeff]
print(['b' + str(c) for c in HB_coeff])
def butter_lowpass(cutoff, fs, order=5):
    sos = signal.butter(10, cutoff, 'lp', fs=fs, output='sos')
    return sos

def butter_lowpass_filter(data, cutoff, fs, order=5):
    sos = butter_lowpass(cutoff, fs, order=order)
    y = signal.sosfilt(sos, data)
    return y

def frequency_plot(wave, F_sample):
    yf = fft(wave)
    xf = fftfreq(int(F_sample *t_interval), 1 / F_sample)
    print("X:",len(xf))
    xf = fftshift(xf)
    yplot = fftshift(yf)
    plt.plot(xf, 1.0/int(F_sample *t_interval) * abs(yplot))
    plt.grid()
    
def fir(signal):
    print(len(signal))
    elements = [0 for _ in range(len(HB_coeff) - 1)]
    elements.extend(signal)
    result = []
    for i in range(len(signal)):
        e = 0
        for j in range(len(HB_coeff)):
            e += HB_coeff[j] * elements[i + len(HB_coeff) - j - 1]
        result.append(e)
    return result[len(HB_coeff):]

def RF(t):
    return np.cos(2 * pi * (F_LO + F_IF) * t + pi / 4)
    
def IM(t):
    return np.cos(2 * pi * (F_LO - F_IF) * t + pi / 4)
    
def mix(signal):
    def I(t):
        return signal(t) * np.cos(2 * pi * F_LO * t)
    def Q(t):
        return signal(t) * np.sin(2 * pi * F_LO * t)
    return I, Q
    
def quantize(s, scale, range):
    return int((s - scale) / range * 31)#TODO
    
def ADC_sampling(sig, F_sample, OLD_F_sample):
    """
        Takes in signals `I` & `Q` sampled at `OLD_F_sample` and resamples them at a new sampling
    frequency `F_sample`.
    """
    sig_sampled = [quantize(s, min(sig), max(sig) - min(sig)) for s in sig[::int(OLD_F_sample//F_sample)]] # resample & quantize I
    num_samples = int(F_sample * t_interval) # determine the number of samples in the time interval
    max_valid_sample = min(num_samples, len(sig_sampled))
    results = np.linspace(0, t_interval, num_samples)[:max_valid_sample], sig_sampled[:max_valid_sample] # remove extraneous elements
    return results


def analog_lowpass(I, Q):
    return butter_lowpass_filter(I, F_IF + MHz(1), analog_F_sample), butter_lowpass_filter(Q, F_IF + MHz(1), analog_F_sample)
    
def hilbert_transform(Q):
    signal = Q
    elements = [0 for _ in range(len(HB_coeff))]
    elements.extend(signal)
    result = []
    for i in range(len(signal)):
        e = 0
        for j in range(len(HB_coeff)):
            e += HB_coeff[j] * elements[i + len(HB_coeff) - j - 1]
        result.append(e)
    return result

t = np.linspace(0, t_interval, num = int(analog_F_sample *t_interval))
I, Q = mix(lambda t: RF(t))
I, Q = I(t), Q(t)
I, Q = analog_lowpass(I, Q)
result = ADC_sampling(I, MHz(20), analog_F_sample)
print("i = ", result[1])
t = result[0]
I = [s - 15 for s in result[1]]
result = ADC_sampling(Q, MHz(20), analog_F_sample)
print("q = ", result[1])
Q = [s - 15 for s in result[1]]
I = [FixedPoint(s, True, 6, 0) for s in I]
Q = [FixedPoint(s, True, 6, 0) for s in Q]

data = [19, -3, -23, -28, -16, 6, 25, 28, 15, -5, -22, -26, -14, 5, 23, 28, 17, -5, -22, -26, -14, 5, 23, 28, 17, -5, -22, -26, -14, 5, 23, 28, 17, -5, -22, -26, -14, 5, 23, 28, 17, -5, -22, -26, -14, 5, 23, 28, 17, -5, -22, -26, -14, 5, 23, 28, 17, -5, -22, -26, -14, 5, 23, 28, 17, -5, -22, -26, -14, 5, 23, 28, 17, -5, -22, -26, -14, 5, 23, 28, 17, -5, -22, -26, -14, 5, 23, 28, 17, -5, -22, -26, -14, 5, 23, 28, 17, -5, -22, -26, -14, 5, 23, 28, 17, -5, -22, -26, -14, 5, 23, 28, 17, -3, -22, -26, -16, 5, 23, 28, 17, -3, -22, -26, -16, 5, 23, 28, 17, -3, -22, -26, -16, 5, 23, 28, 17, -3, -22, -26, -16, 5, 23, 28, 17, -3, -22, -26, -16, 5, 23, 28, 17, -3, -22, -26, -16, 5, 23, 28, 17, -3, -22, -26, -16, 5, 23, 28, 17, -3, -22, -26]

ht = hilbert_transform(Q)
#plt.plot(list(range(len(data))), data)
#plt.plot(t, ht)
#plt.plot(t, Q)
#plt.plot(t, [(I[t] - ht[t]).__float__() for t in range(len(t))])
dataI = [1.8239016011122513E-8, 0.1139240101227716, 0.18524488148177495, -0.1517343021105487, -0.35737834640448457, -0.4904510785485695, -0.4729574930800446, -0.3230877673179814, -0.07976983200392725, 0.1862075823982618, 0.3988508253269442, 0.49722809758308595, 0.4531232112910137, 0.2791955579638164, 0.025267187190617702, -0.2358989184376714, -0.42947492824938216, -0.4999953263749513, -0.42725474882944897, -0.23209496048526737, 0.029565797239837408, 0.19985870913720874, -0.4690033433131523, -0.4749226373848918, -0.23568230594891112, 0.09163952516753737, 0.3898610702267557, 0.499107464145518, 0.37920864619979505, 0.08101612649442827, -0.25428267017372863, -0.471328213040575, -0.46859191290840524, -0.2474899627488821, 0.08897718943838358, 0.3839686718896935, 0.4999994225251109, 0.38298719376619145, 0.087470276273925, -0.24881527689305077, -0.4691316424189417, -0.470861763049663, -0.2759257529779643, -0.021926134428619442, 0.238767857107883, 0.43126900092579223, 0.49992146511736646, 0.4255217539363006, 0.2290987298981994, -0.03291865149283844, -0.28552544570597144, -0.4563130367037057, -0.4963592043538012, -0.39418469266570433, -0.1790669582857752, 0.08735812749194094, 0.32875284776428165, 0.4759517017613273, 0.48677854100148993, 0.35813118012750655, 0.12687029840455052, -0.14064758054037976, 0.05220588470498062, 0.5801386507647244, 0.3671934269136446, 0.1160177675381052, -0.23519909947695564, -0.4611783393874989, -0.47716560897240623, -0.2693297553295009, 0.06369602290866742, 0.36708077209884504, 0.49938955610237107, 0.3989217862768943, 0.11253388207790412, -0.2263104329097703, -0.45967192095872617, -0.47878819111167076, -0.2747476647776625, 0.05734855429645866, 0.36271552934370804, 0.49902601280405967, 0.4050948254236373, 0.17296693313070483, -0.08961840583534446, -0.33165820708574384, -0.4768433848945457, -0.48603957931874164, -0.3557637279283643, -0.12361609118310952, 0.14396748835365536, 0.37029745946715537, 0.4905279835216176, 0.4702105026592985, 0.3151656335184166, 0.06981813464131206, -0.19553408839136105, -0.40486086314143466, -0.4981849465574338, -0.4487666126130425, -0.27076544081546583, -0.015183234888891867, 0.21329225864130863, -0.3709227367119629, -0.5171116888325072, -0.2755012166272652, 0.03147575126188146, 0.3521434185123208, 0.4962804391710768, 0.4142809694709359, 0.13712575221087805, -0.20314891191074894, -0.4490315061509956, -0.48552343883283117, -0.2957547258998256, 0.03187193955610008, 0.3446404473668376, 0.4967777559736645, 0.41737400869025215, 0.14343823563839203, -0.19735198078707972, -0.44615933446968026, -0.48702248954606736, -0.3150323909989775, -0.06557840695708168, 0.19818758559783112, 0.4070361656711093, 0.49836587084938677, 0.44731283190146637, 0.26791494104606206, 0.011826467872952422, -0.24767980060005892, -0.4362084815871816, -0.4997566407646839, -0.4201106498467763, -0.22009294094247353, 0.042987031821121534, 0.2937500872465769, 0.460346441658661, 0.4950420733739149, 0.3878957878597682, 0.16960769458847086, -0.09727721492634026, -0.07982313359816479, 0.5662607621413756, 0.40619325433283615, 0.16782594179586602, -0.1809677289176675, -0.43704209059878024, -0.4908958626958753, -0.31608276919871353, 0.00636993931885629, 0.3256336630681864, 0.4932431593149043, 0.4309019706704817, 0.16774912767414638, -0.17359995924482907, -0.43403225893206165, -0.4921700668660074, -0.32091387470413, -8.46029489342057E-5, 0.32078419869260566, 0.49214008744515114, 0.4349198579079595, 0.21429029492769652, -0.045441010763981805, -0.2967469529641471, -0.4615731804717958, -0.494573507707776, -0.3857638980117256, -0.16643707241591701, 0.10057342557210065, 0.3387726972758154, 0.4799016269599787, 0.4835286206794339, 0.34861174466338396, 0.11380927465512836, -0.153602526826842, -0.3770033131374741, -0.49238329477088694, -0.46668316313287517, -0.30726666569124833, -0.05981062828866833, 0.19763892909271927, -0.24370598015165887, -0.554639467793006, -0.31186129062827245, -0.028677979403373813, 0.3094653546997164, 0.4870466434259268, 0.4437904557491375, 0.19145536426425394, -0.1493408493046518, -0.4207845347721194, -0.4960300511462765, -0.34010345736792424, -0.02565554232953648, 0.3007500428129702, 0.48697979319312823, 0.4462357120768114, 0.1975073828698117, -0.14327617614893506, -0.41728084403274035
]

dataQ = [0.0, -0.13814944660769332, -0.5209261869997771, -0.469547394303771, -0.34706412801113146, -0.10577665211646714, 0.16030191693694895, 0.381714911881487, 0.4936235523349912, 0.46401067387808476, 0.3015317328247131, 0.052605412519761675, -0.21136771387209613, -0.41479075395347337, -0.4993606470702265, -0.4408534610016373, -0.25602989818220767, 0.002152300099541757, 0.259717914567742, 0.442867824736213, 0.49912509716841613, 0.33374241248112574, -0.2199499240097309, 0.19388986597342847, 0.4356109359185133, 0.4901357247224482, 0.31617364577572526, -0.007695205798673342, -0.3260300728526803, -0.493498272148429, -0.4304192207785853, -0.16696997527737983, 0.174404990953229, 0.43445007897242877, 0.49202082913197553, 0.32026150073993825, -7.648139515416502E-4, -0.3214354358584745, -0.4922894043393525, -0.4336944502425386, -0.1729607428915484, 0.16833021656716982, 0.4207314290078084, 0.49822001812960004, 0.4396715397750324, 0.2530228971939769, -0.005495408462398, -0.2625862634317549, -0.44442298105739036, -0.4989122899453745, -0.410460122678264, -0.20439523055948045, 0.06023110566435818, 0.30760107822683086, 0.4668350649925523, 0.49230942732633465, 0.37672477533640836, 0.15319909979414065, -0.11422194310743311, -0.3489155316215615, -0.48363614217063067, -0.47972305953073086, -0.046068888231911964, 0.11492178503809271, -0.34150660350242273, -0.47542494714498723, -0.4476682978713031, -0.18920409323699253, 0.14967713668570257, 0.4213838457296259, 0.49588628863168677, 0.33948673604942137, 0.02480750933899348, -0.30142958085551025, -0.48717078988143425, -0.44585233677573727, -0.19672638471267276, 0.14408978664439626, 0.4177483089014501, 0.49670015602404893, 0.34414729969891295, 0.0311923355795425, -0.29467123828099373, -0.4712450050260426, -0.4904908985734036, -0.37500751591444326, -0.14979270772707173, 0.11741844759984899, 0.35134513663072264, 0.4844691334248763, 0.4788282955043682, 0.3359750288394949, 0.09686170711440915, -0.17000643071141644, -0.38816309325432796, -0.49510141503908295, -0.46018086695244764, -0.29340699446015006, -0.042564708837008886, 0.2204734119490441, 0.4203404108858017, 0.49976941763292304, 0.4100831682483552, -0.18058820947722215, 0.1075977149432493, 0.4170719946589444, 0.493135829113781, 0.3604078734070265, 0.04915461125209794, -0.2801420927046117, -0.4809772365525826, -0.4567652760404728, -0.2200064529474149, 0.11942557435866877, 0.4031469678324142, 0.4989842414426106, 0.36224653552135794, 0.05667270540754223, -0.2753159414847707, -0.47898371035551296, -0.45940407217772, -0.22570284096049736, 0.11319103235514953, 0.39182381764280994, 0.4950755251778548, 0.45892810915717674, 0.29071222825646015, 0.039171708528611196, -0.22346322848453007, -0.4221654669991078, -0.4998543161187322, -0.43434793587255993, -0.24437858527209064, 0.015606466226027961, 0.27112184535076034, 0.4489532724439163, 0.49814870637945047, 0.4046120059125752, 0.19514389812862143, -0.07023779478315058, -0.3154946019661225, -0.4703543617175274, -0.49044585842391, -0.1578005071020507, 0.1830927868117947, -0.29484568359426466, -0.46088315581660355, -0.47055415282056196, -0.24152405716620154, 0.09415563266397105, 0.3875363512643181, 0.4999711200051192, 0.3793843476756406, 0.0820135280476855, -0.2536150595402889, -0.47101901677755464, -0.4688968072010331, -0.2482244797065916, 0.0881399316218946, 0.3834243319076914, 0.49999989031268494, 0.38353281326689054, 0.08830660897869079, -0.24747331076694484, -0.45482204073420524, -0.496302017505102, -0.40321169600842116, -0.1918303574616737, 0.07349492934047074, 0.3181205585961388, 0.47147813051568577, 0.48978191306440627, 0.3677410157289662, 0.14033525939622604, -0.1272801794815618, -0.3584268682948617, -0.4868751591445481, -0.4758216901491491, -0.3284333713955233, -0.08694073663671888, 0.17946258269426263, 0.39444541307366154, 0.49640980078375546, 0.4490998843518647, -0.12091702252805185, 0.01967531097819167, 0.39206925571280515, 0.49032343627980657, 0.39946588675408834, 0.10555406784078106, -0.2306368042570114, -0.4620499757050013, -0.47708078587484287, -0.27012421791546937, 0.06286283859144468, 0.3665079343263725, 0.4993418826478227, 0.39943631630540943, 0.11335997079973976, -0.2255518569239653, -0.4593373114848031, -0.47903218397144504, -0.27545712545778567]
plt.plot([i for i in range(len(dataI))], dataI)
plt.plot([i for i in range(len(dataQ))], dataQ)
#print([I[t] - ht[t] for t in range(len(t))])
#plt.plot(t, I)
plt.show()
