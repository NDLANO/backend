package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V74__StripWhitespaceAfterPeriodTest extends UnitSuite with TestEnvironment {
  val migration = new V74__StripWhitespaceAfterPeriod()

  val originalArticle: String =
    """<section>
      |    <h2>Generelt om vurdering av eksamen</h2>
      |    <p>Skriftlig eksamen i fremmedspråk består av to deler: lesing og skriving. Kandidaten får poeng for hver oppgave, og summen av disse poengene gir den endelige karakteren.</p>
      |    <p>Lesing teller 35 prosent av totalkarakteren, mens skriving teller 65 prosent. NB! Kandidaten må bestå både lesedelen og skrivedelen for å bestå hele eksamen (dvs. få minst karakteren 2 i begge).</p>
      |    <p>Utdanningsdirektoratet utarbeider hvert år eksamens- og sensorveiledninger. Vurderingskriteriene og poengskalaene kan variere fra år til år. Her tar vi utgangspunkt i følgende veiledninger: </p>
      |    <ul>
      |        <li>
      |            <p>Eksamensveiledning for fremmedspråk nivå I og II, vår 2025</p>
      |        </li>
      |        <li>
      |            <p>Veiledning for sensur av oppgaver i tysk I, II og III etter LK20 (fra 2024)</p>
      |        </li>
      |    </ul>
      |    <p>Begge veiledningene ligger på nettsidene til Utdanningsdirektoratet.</p>
      |    <h2>Del 1: Lesing</h2>
      |    <p>Leseforståelsesoppgavene på eksamen rettes og vurderes automatisk. I vårt eksamenssett får elevene oppgitt hvor mange poeng de oppnår på hver oppgave, men poengene blir ikke summert automatisk eller gjort om til en karakter. Derfor bør elevene skrive ned poengene de får når de svarer på oppgavene, og summere dem til slutt. </p>
      |    <p>Tabell A viser en poengskala for leseforståelsesdelen. I oppgavesettet vårt kan eleven oppnå maksimalt 30 poeng: 4 poeng i oppgave 1, 8 poeng i oppgave 2, 10 poeng i oppgave 3 og 8 poeng i oppgave 4.</p>
      |    <table>
      |        <caption>Tabell A: poengskala for del 1</caption>
      |        <thead>
      |            <tr>
      |                <th scope="col" data-align="center">
      |                    <p>Antall poeng</p>
      |                </th>
      |                <th scope="col" data-align="center">
      |                    <p>Karakter</p>
      |                </th>
      |            </tr>
      |        </thead>
      |        <tbody>
      |            <tr>
      |                <td data-align="center">28–30</td>
      |                <td data-align="center">6</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">23–27</td>
      |                <td data-align="center">5</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">18–22</td>
      |                <td data-align="center">4</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">14–17</td>
      |                <td data-align="center">3</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">10–13</td>
      |                <td data-align="center">2</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">0–9</td>
      |                <td data-align="center">1</td>
      |            </tr>
      |        </tbody>
      |    </table>
      |    <h2>Del 2: Skriving</h2>
      |    <p>Elevsvara i denne delen må vurderes av en sensor. Tidligere beskrev vurderingskriteriene høy, middels og lav måloppnåelse. Nå gis det fra null til fem poeng per skriveoppgave, og vurderingskriteriene beskriver for hvert poengtall hva det er elevene mestrer. Skriveoppgavene blir vurdert både enkeltvis og i sin helhet.</p>
      |    <p>Det er lurt å trene elevene på å</p>
      |    <ul>
      |        <li>
      |            <p>ha med alle punkter som oppgaven spør om</p>
      |        </li>
      |        <li>
      |            <p>skrive det antall ord som oppgaven krever</p>
      |        </li>
      |        <li>
      |            <p>kontrollere språket slik at kvaliteten på svaret blir bedre</p>
      |        </li>
      |        <li>
      |            <p>bruke et passende og variert vokabular</p>
      |        </li>
      |        <li>
      |            <p>legge seg på et overkommelig nivå når det gjelder setningsstruktur framfor å bygge avanserte setninger med flere ledd som de ikke klarer å håndtere</p>
      |        </li>
      |    </ul>
      |    <h3>Vurdering av hver enkelt skriveoppgave</h3>
      |    <p>Hver skriveoppgave blir vurdert individuelt. De sentrale kriteriene er </p>
      |    <ul>
      |        <li>
      |            <p>om teksten svarer på oppgaven (inntil 1 poeng)</p>
      |        </li>
      |        <li>
      |            <p>hvor godt den kommuniserer (inntil 4 poeng)</p>
      |        </li>
      |    </ul>
      |    <p>Bruk tabell B for å vurdere de enkelte elevsvara. </p>
      |    <table>
      |        <caption>Tabell b: vurdering av hver enkelt skriveoppgave (maks 5 poeng per oppgave)</caption>
      |        <thead>
      |            <tr>
      |                <th scope="col" id="00"></th>
      |                <th scope="col" id="01" data-align="right">
      |                    <p>4 poeng</p>
      |                </th>
      |                <th scope="col" id="02" data-align="right">
      |                    <p>3 poeng</p>
      |                </th>
      |                <th scope="col" id="03" data-align="right">
      |                    <p>2 poeng</p>
      |                </th>
      |                <th scope="col" id="04" data-align="right">
      |                    <p>1 poeng</p>
      |                </th>
      |                <th scope="col" id="05" data-align="right">
      |                    <p>0 poeng</p>
      |                </th>
      |            </tr>
      |        </thead>
      |        <tbody>
      |            <tr>
      |                <th scope="row" id="r1">
      |                    <p>Svarer teksten på oppgaven?</p>
      |                </th>
      |                <td headers="01 r1"></td>
      |                <td headers="02 r1"></td>
      |                <td headers="03 r1"></td>
      |                <td headers="04 r1">Teksten svarer på alt i oppgavebestillinga.</td>
      |                <td headers="05 r1">Teksten svarer ikke på alt i oppgavebestillinga, eller teksten mangler.</td>
      |            </tr>
      |            <tr>
      |                <th scope="row" id="r2">
      |                    <p>Kommunikasjon*</p>
      |                </th>
      |                <td headers="01 r2">
      |                    <p>Kommunikasjonen er tydelig.</p>
      |                    <p>Teksten er ikke feilfri, men er svært enkel å forstå.</p>
      |                </td>
      |                <td headers="02 r2">
      |                    <p>Kommunikasjonen er stort sett tydelig. </p>
      |                    <p>Teksten har noen feil, men er stort sett lett å forstå og har god flyt.</p>
      |                </td>
      |                <td headers="03 r2">
      |                    <p>Kommunikasjonen er delvis tydelig.</p>
      |                    <p>Teksten er delvis vanskelig å forstå på grunn av feil.</p>
      |                </td>
      |                <td headers="04 r2">
      |                    <p>Kommunikasjonen er kun noen ganger tydelig. </p>
      |                    <p>Teksten er oppstykka og/eller usammenhengende, og den er vanskelig å forstå.</p>
      |                </td>
      |                <td headers="05 r2">Besvarelsen kommuniserer ikke godt nok** til å få uttelling.</td>
      |            </tr>
      |        </tbody>
      |    </table>
      |    <p><em>*Kommunikasjon handler om i hvilken grad teksten formidler et relevant innhold på en forståelig måte. Dersom kandidaten ikke svarer på alle ledd i oppgavebestillingen, vil ikke kandidaten kunne få full uttelling. </em></p>
      |    <p><em>** Noen få setninger kommuniserer på en enkel måte og inneholder "relevante" enkeltord. De fleste setninger gir ikke mening eller er ufullstendige. </em></p>
      |    <h3>Helhetsvurdering av skriftlig kompetanse</h3>
      |    <p>Her blir elevens skrivekompetanse vurdert i sin helhet. Det gis poeng for grammatikk, setningsstruktur, vokabular, rettskriving og evne til å utføre språkhandlinger: </p>
      |    <ul>
      |        <li>
      |            <p>inntil 3 poeng for grammatikk</p>
      |        </li>
      |        <li>
      |            <p>inntil 4 poeng for setningsstruktur</p>
      |        </li>
      |        <li>
      |            <p>0,1,2 eller 4 poeng for vokabular (det kan altså ikke gis 3 poeng her)</p>
      |        </li>
      |        <li>
      |            <p>inntil 2 poeng for rettskrivning</p>
      |        </li>
      |        <li>
      |            <p>inntil 4 poeng for språkhandlinger</p>
      |        </li>
      |    </ul>
      |    <p>Bruk tabell C for å vurdere den samla språklige kvaliteten i alle skriveoppgavene. </p>
      |    <table>
      |        <caption>Tabell c: helhetlig skrivekompetanse (maks 17 poeng)</caption>
      |        <thead>
      |            <tr>
      |                <th scope="col" id="00"></th>
      |                <th scope="col" id="01" data-align="right">
      |                    <p>4 poeng</p>
      |                </th>
      |                <th scope="col" id="02" data-align="right">
      |                    <p>3 poeng</p>
      |                </th>
      |                <th scope="col" id="03" data-align="right">
      |                    <p>2 poeng</p>
      |                </th>
      |                <th scope="col" id="04" data-align="right">
      |                    <p>1 poeng</p>
      |                </th>
      |                <th scope="col" id="05" data-align="right">
      |                    <p>0 poeng</p>
      |                </th>
      |            </tr>
      |        </thead>
      |        <tbody>
      |            <tr>
      |                <th scope="row" id="r1">
      |                    <p>Grammatikk*</p>
      |                </th>
      |                <td headers="01 r1">---</td>
      |                <td headers="02 r1">Grammatikken er stort sett korrekt.</td>
      |                <td headers="03 r1">Grammatikken er delvis korrekt.</td>
      |                <td headers="04 r1">Grammatikken er ofte ukorrekt.</td>
      |                <td headers="05 r1">Grammatikken i besvarelsen ligger under det nivået som kan forventes på nivå II.</td>
      |            </tr>
      |            <tr>
      |                <th scope="row" id="r2">
      |                    <p>Setningsstruktur</p>
      |                </th>
      |                <td headers="01 r2">Setningsstrukturen er korrekt. </td>
      |                <td headers="02 r2">Setningsstrukturen er stort sett korrekt.</td>
      |                <td headers="03 r2">Flere setninger har ukorrekt struktur.</td>
      |                <td headers="04 r2">Mange setninger har ukorrekt struktur.</td>
      |                <td headers="05 r2">Setningsstrukturen i besvarelsen ligger under det nivået som kan forventes på nivå II.</td>
      |            </tr>
      |            <tr>
      |                <th scope="row" id="r3">
      |                    <p>Vokabular</p>
      |                </th>
      |                <td headers="01 r3">Vokabularet er variert og passer godt inn i sammenhengen.**</td>
      |                <td headers="02 r3">---</td>
      |                <td headers="03 r3">Vokabularet er mindre variert og/eller passer ikke alltid godt inn i sammenhengen.</td>
      |                <td headers="04 r3">Vokabularet er upresist og passer ofte dårlig inn i sammenhengen.</td>
      |                <td headers="05 r3">Vokabularet i besvarelsen ligger under det nivået som kan forventes på nivå II.</td>
      |            </tr>
      |            <tr>
      |                <th scope="row" id="r4">
      |                    <p>Rettskriving</p>
      |                </th>
      |                <td headers="01 r4">---</td>
      |                <td headers="02 r4">---</td>
      |                <td headers="03 r4">Rettskrivingen er korrekt.</td>
      |                <td headers="04 r4">Rettskrivingen inneholder få feil.</td>
      |                <td headers="05 r4">Rettskrivingen i besvarelsen ligger under det som kan forventes på nivå II.</td>
      |            </tr>
      |            <tr>
      |                <th scope="row" id="r5">
      |                    <p>Språkhandlinger</p>
      |                </th>
      |                <td headers="01 r5">Informerer, forteller, beskriver og begrunner på en selvstendig måte.</td>
      |                <td headers="02 r5">Informerer, forteller, beskriver og begrunner på en god måte.</td>
      |                <td headers="03 r5">Informerer, forteller, beskriver og begrunner på en enkel måte.</td>
      |                <td headers="04 r5">Informerer, forteller, beskriver og begrunner på en svært enkel måte.</td>
      |                <td headers="05 r5">Svaret gir ikke tilstrekkelig grunnlag for å vurdere språkhandlinger.</td>
      |            </tr>
      |        </tbody>
      |    </table>
      |    <p><em>* Med grammatikk menes for eksempel verbbruk i person og tid som er relevant med tanke på oppgavene og nivå, adjektiv, kasus, </em>
      |        <ndlaembed data-content-id="8561" data-link-text="register" data-resource="concept" data-type="inline"></ndlaembed><em>, morfologi osv. </em>
      |    </p>
      |    <p><em>** For å få 4 poeng for vokabularet må begge kriterier oppfylles.</em></p>
      |    <h2>Eksempel på karaktersetting</h2>
      |    <p>Her kommer et eksempel på hvordan du vurderer eksamensoppgavene og regner ut sluttkarakteren.</p>
      |    <h3>Del 1: Lesing</h3>
      |    <p>En elev har oppnådd <strong>21 av 30 poeng</strong> i del 1. Ifølge tabell A får eleven altså karakteren 4 på lesedelen. </p>
      |    <h3>Del 2: Skrivedel</h3>
      |    <h4>Vurdering av hver enkelt oppgave</h4>
      |    <p>Først vurderer man hver av de tre skriveoppgave individuelt ved hjelp av tabell B.</p>
      |    <ul>
      |        <li>
      |            <p>Eleven får <strong>2 poeng</strong> i skriveoppgave 1. Teksten svarer på alt i oppgavebestillingen, men er usammenhengende og vanskelig å forstå.</p>
      |        </li>
      |        <li>
      |            <p>I skriveoppgave 2 har ikke eleven svart på alle deler av oppgaven. Teksten har også feil som gjør at den er delvis vanskelig å forstå. Eleven får da <strong>2 poeng</strong> på denne oppgaven.</p>
      |        </li>
      |        <li>
      |            <p>I skriveoppgave 3 svarer eleven på alle deler av oppgaven. Det er noen feil i teksten, men stort sett har den god flyt og er lett å forstå. Eleven får <strong>4 poeng</strong>.</p>
      |        </li>
      |    </ul>
      |    <p>Totalt får eleven altså <strong>8 av 15 mulige poeng</strong>.</p>
      |    <h4>Helhetsvurdering</h4>
      |    <p>Så bruker du tabell C for å vurdere elevens skrivekompetanse i sin helhet. Eleven i eksempelet vårt får <strong>2 poeng</strong> for grammatikk, <strong>3 poeng</strong> for setningsstruktur, <strong>2 poeng</strong> for vokabular, <strong>1 poeng </strong>for rettskriving og <strong>2 poeng</strong> for språkhandlinger.</p>
      |    <p>Totalt får eleven altså <strong>10 av 17 mulige poeng</strong>.</p>
      |    <h4>Karakter på del 2</h4>
      |    <p>Etter at alle oppgavene er vurdert, både individuelt og helhetlig, blir poengene for skrivedelen summert. Karakteren på denne delen settes ut fra en poengskala (se tabell D nedenfor). Eleven eksempelet vårt har fått <strong>18 av 32 mulige</strong> <strong>poeng </strong>og får dermed karakteren 3 på del 2.</p>
      |    <table>
      |        <caption>Tabell D: poengskala for Del 2</caption>
      |        <thead>
      |            <tr>
      |                <th scope="col">
      |                    <p>Sum av poeng for skriving</p>
      |                </th>
      |                <th scope="col">
      |                    <p>Karakter</p>
      |                </th>
      |            </tr>
      |        </thead>
      |        <tbody>
      |            <tr>
      |                <td data-align="center">30–32</td>
      |                <td data-align="center">6</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">25–29</td>
      |                <td data-align="center">5</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">20–24</td>
      |                <td data-align="center">4</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">14–19</td>
      |                <td data-align="center">3</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">10–13</td>
      |                <td data-align="center">2</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">0–9</td>
      |                <td data-align="center">1</td>
      |            </tr>
      |        </tbody>
      |    </table>
      |    <h3>Samla vurdering</h3>
      |    <p>For å regne ut samla karakter gjelder følgende regnestykke: <strong>karakter på del 1</strong>&#xa0;<strong>+</strong>&#xa0;<strong>karakter på del 2</strong>&#xa0;<strong>+</strong>&#xa0;<strong>karakter på del 2</strong>. Så bruker man tabellen nedenfor for å avgjøre den samla karakteren.</p>
      |    <p>Husk at begge delene må være bestått for å bestå hele eksamen. Om en elev for eksempel har fått karakteren 6 på del 1 og karakteren 1 på del 2, vurderes hele eksamen som ikke bestått.</p>
      |    <p>Eleven i eksempelet vårt har fått karakteren 4 på del 1 og karakteren 3 på del 2. Da blir regnestykket slikt: <strong>4</strong>&#xa0;<strong>+</strong>&#xa0;<strong>3</strong>&#xa0;<strong>+</strong>&#xa0;<strong>3</strong>&#xa0;<strong>=</strong>&#xa0;<strong>10</strong>. I tabell E ser vi at summen 10 tilsvarer karakter 3. Eleven får altså 3 som samla karakter på hele eksamen.</p>
      |    <table>
      |        <caption>tabell E: samla karakter på del 1 og del 2</caption>
      |        <thead>
      |            <tr>
      |                <th scope="col" data-align="center">
      |                    <p>Totalsum</p>
      |                </th>
      |                <th scope="col" data-align="center">
      |                    <p>Karakter</p>
      |                </th>
      |            </tr>
      |        </thead>
      |        <tbody>
      |            <tr>
      |                <td data-align="center">17–18</td>
      |                <td data-align="center">6</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">14–16</td>
      |                <td data-align="center">5</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">11–13</td>
      |                <td data-align="center">4</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">8–10</td>
      |                <td data-align="center">3</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">6–7</td>
      |                <td data-align="center">2</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">0–5</td>
      |                <td data-align="center">1</td>
      |            </tr>
      |        </tbody>
      |    </table>
      |    <details>
      |        <summary>
      |            <h2>Kilder</h2>
      |        </summary>
      |        <p>Utdanningsdirektoratet. (2024, 3. april). <em>Forberede og ta eksamen</em>. <a href="https://www.udir.no/eksamen-og-prover/eksamen/forberede-og-ta-eksamen/">https://www.udir.no/eksamen-og-prover/eksamen/forberede-og-ta-eksamen/ </a> </p> <p>Utdanningsdirektoratet. (u.å.). <em>Eksamensveiledning for fremmedspråk I og II, 2025</em>. Tilgjengelig via <a href="https://sokeresultat.udir.no/eksamensoppgaver.html?start=1&amp;query=fremmedspr%C3%A5k%20I%20og%20II&amp;ExKL=Kunnskapsl%C3%B8ftet%202020">https://sokeresultat.udir.no/eksamensoppgaver.html?start=1&amp;query=fremmedspr%C3%A5k%20I%20og%20II&amp;ExKL=Kunnskapsl%C3%B8ftet%202020 </a></p>
      |        <p>Utdanningsdirektoratet. (u.å.). <em>Veiledning for sensur av oppgaver i spansk I, II og III etter LK20, 2024</em>. Tilgjengelig via <em>Eksamensveiledning for fremmedspråk I og II, 2024</em> fra <a href="https://sokeresultat.udir.no/eksamensoppgaver.html?start=1&amp;query=fremmedspr%C3%A5k%20I%20og%20II&amp;ExKL=Kunnskapsl%C3%B8ftet%202020">https://sokeresultat.udir.no/eksamensoppgaver.html?start=1&amp;query=fremmedspr%C3%A5k%20I%20og%20II&amp;ExKL=Kunnskapsl%C3%B8ftet%202020</a></p>
      |	   </details>
      |</section>""".stripMargin

  val migratedArticle: String =
    """<section>
      |    <h2>Generelt om vurdering av eksamen</h2>
      |    <p>Skriftlig eksamen i fremmedspråk består av to deler: lesing og skriving. Kandidaten får poeng for hver oppgave, og summen av disse poengene gir den endelige karakteren.</p>
      |    <p>Lesing teller 35 prosent av totalkarakteren, mens skriving teller 65 prosent. NB! Kandidaten må bestå både lesedelen og skrivedelen for å bestå hele eksamen (dvs. få minst karakteren 2 i begge).</p>
      |    <p>Utdanningsdirektoratet utarbeider hvert år eksamens- og sensorveiledninger. Vurderingskriteriene og poengskalaene kan variere fra år til år. Her tar vi utgangspunkt i følgende veiledninger: </p>
      |    <ul>
      |        <li>
      |            <p>Eksamensveiledning for fremmedspråk nivå I og II, vår 2025</p>
      |        </li>
      |        <li>
      |            <p>Veiledning for sensur av oppgaver i tysk I, II og III etter LK20 (fra 2024)</p>
      |        </li>
      |    </ul>
      |    <p>Begge veiledningene ligger på nettsidene til Utdanningsdirektoratet.</p>
      |    <h2>Del 1: Lesing</h2>
      |    <p>Leseforståelsesoppgavene på eksamen rettes og vurderes automatisk. I vårt eksamenssett får elevene oppgitt hvor mange poeng de oppnår på hver oppgave, men poengene blir ikke summert automatisk eller gjort om til en karakter. Derfor bør elevene skrive ned poengene de får når de svarer på oppgavene, og summere dem til slutt.</p>
      |    <p>Tabell A viser en poengskala for leseforståelsesdelen. I oppgavesettet vårt kan eleven oppnå maksimalt 30 poeng: 4 poeng i oppgave 1, 8 poeng i oppgave 2, 10 poeng i oppgave 3 og 8 poeng i oppgave 4.</p>
      |    <table>
      |        <caption>Tabell A: poengskala for del 1</caption>
      |        <thead>
      |            <tr>
      |                <th scope="col" data-align="center">
      |                    <p>Antall poeng</p>
      |                </th>
      |                <th scope="col" data-align="center">
      |                    <p>Karakter</p>
      |                </th>
      |            </tr>
      |        </thead>
      |        <tbody>
      |            <tr>
      |                <td data-align="center">28–30</td>
      |                <td data-align="center">6</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">23–27</td>
      |                <td data-align="center">5</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">18–22</td>
      |                <td data-align="center">4</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">14–17</td>
      |                <td data-align="center">3</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">10–13</td>
      |                <td data-align="center">2</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">0–9</td>
      |                <td data-align="center">1</td>
      |            </tr>
      |        </tbody>
      |    </table>
      |    <h2>Del 2: Skriving</h2>
      |    <p>Elevsvara i denne delen må vurderes av en sensor. Tidligere beskrev vurderingskriteriene høy, middels og lav måloppnåelse. Nå gis det fra null til fem poeng per skriveoppgave, og vurderingskriteriene beskriver for hvert poengtall hva det er elevene mestrer. Skriveoppgavene blir vurdert både enkeltvis og i sin helhet.</p>
      |    <p>Det er lurt å trene elevene på å</p>
      |    <ul>
      |        <li>
      |            <p>ha med alle punkter som oppgaven spør om</p>
      |        </li>
      |        <li>
      |            <p>skrive det antall ord som oppgaven krever</p>
      |        </li>
      |        <li>
      |            <p>kontrollere språket slik at kvaliteten på svaret blir bedre</p>
      |        </li>
      |        <li>
      |            <p>bruke et passende og variert vokabular</p>
      |        </li>
      |        <li>
      |            <p>legge seg på et overkommelig nivå når det gjelder setningsstruktur framfor å bygge avanserte setninger med flere ledd som de ikke klarer å håndtere</p>
      |        </li>
      |    </ul>
      |    <h3>Vurdering av hver enkelt skriveoppgave</h3>
      |    <p>Hver skriveoppgave blir vurdert individuelt. De sentrale kriteriene er </p>
      |    <ul>
      |        <li>
      |            <p>om teksten svarer på oppgaven (inntil 1 poeng)</p>
      |        </li>
      |        <li>
      |            <p>hvor godt den kommuniserer (inntil 4 poeng)</p>
      |        </li>
      |    </ul>
      |    <p>Bruk tabell B for å vurdere de enkelte elevsvara.</p>
      |    <table>
      |        <caption>Tabell b: vurdering av hver enkelt skriveoppgave (maks 5 poeng per oppgave)</caption>
      |        <thead>
      |            <tr>
      |                <th scope="col" id="00"></th>
      |                <th scope="col" id="01" data-align="right">
      |                    <p>4 poeng</p>
      |                </th>
      |                <th scope="col" id="02" data-align="right">
      |                    <p>3 poeng</p>
      |                </th>
      |                <th scope="col" id="03" data-align="right">
      |                    <p>2 poeng</p>
      |                </th>
      |                <th scope="col" id="04" data-align="right">
      |                    <p>1 poeng</p>
      |                </th>
      |                <th scope="col" id="05" data-align="right">
      |                    <p>0 poeng</p>
      |                </th>
      |            </tr>
      |        </thead>
      |        <tbody>
      |            <tr>
      |                <th scope="row" id="r1">
      |                    <p>Svarer teksten på oppgaven?</p>
      |                </th>
      |                <td headers="01 r1"></td>
      |                <td headers="02 r1"></td>
      |                <td headers="03 r1"></td>
      |                <td headers="04 r1">Teksten svarer på alt i oppgavebestillinga.</td>
      |                <td headers="05 r1">Teksten svarer ikke på alt i oppgavebestillinga, eller teksten mangler.</td>
      |            </tr>
      |            <tr>
      |                <th scope="row" id="r2">
      |                    <p>Kommunikasjon*</p>
      |                </th>
      |                <td headers="01 r2">
      |                    <p>Kommunikasjonen er tydelig.</p>
      |                    <p>Teksten er ikke feilfri, men er svært enkel å forstå.</p>
      |                </td>
      |                <td headers="02 r2">
      |                    <p>Kommunikasjonen er stort sett tydelig.</p>
      |                    <p>Teksten har noen feil, men er stort sett lett å forstå og har god flyt.</p>
      |                </td>
      |                <td headers="03 r2">
      |                    <p>Kommunikasjonen er delvis tydelig.</p>
      |                    <p>Teksten er delvis vanskelig å forstå på grunn av feil.</p>
      |                </td>
      |                <td headers="04 r2">
      |                    <p>Kommunikasjonen er kun noen ganger tydelig.</p>
      |                    <p>Teksten er oppstykka og/eller usammenhengende, og den er vanskelig å forstå.</p>
      |                </td>
      |                <td headers="05 r2">Besvarelsen kommuniserer ikke godt nok** til å få uttelling.</td>
      |            </tr>
      |        </tbody>
      |    </table>
      |    <p><em>*Kommunikasjon handler om i hvilken grad teksten formidler et relevant innhold på en forståelig måte. Dersom kandidaten ikke svarer på alle ledd i oppgavebestillingen, vil ikke kandidaten kunne få full uttelling. </em></p>
      |    <p><em>** Noen få setninger kommuniserer på en enkel måte og inneholder "relevante" enkeltord. De fleste setninger gir ikke mening eller er ufullstendige. </em></p>
      |    <h3>Helhetsvurdering av skriftlig kompetanse</h3>
      |    <p>Her blir elevens skrivekompetanse vurdert i sin helhet. Det gis poeng for grammatikk, setningsstruktur, vokabular, rettskriving og evne til å utføre språkhandlinger: </p>
      |    <ul>
      |        <li>
      |            <p>inntil 3 poeng for grammatikk</p>
      |        </li>
      |        <li>
      |            <p>inntil 4 poeng for setningsstruktur</p>
      |        </li>
      |        <li>
      |            <p>0,1,2 eller 4 poeng for vokabular (det kan altså ikke gis 3 poeng her)</p>
      |        </li>
      |        <li>
      |            <p>inntil 2 poeng for rettskrivning</p>
      |        </li>
      |        <li>
      |            <p>inntil 4 poeng for språkhandlinger</p>
      |        </li>
      |    </ul>
      |    <p>Bruk tabell C for å vurdere den samla språklige kvaliteten i alle skriveoppgavene.</p>
      |    <table>
      |        <caption>Tabell c: helhetlig skrivekompetanse (maks 17 poeng)</caption>
      |        <thead>
      |            <tr>
      |                <th scope="col" id="00"></th>
      |                <th scope="col" id="01" data-align="right">
      |                    <p>4 poeng</p>
      |                </th>
      |                <th scope="col" id="02" data-align="right">
      |                    <p>3 poeng</p>
      |                </th>
      |                <th scope="col" id="03" data-align="right">
      |                    <p>2 poeng</p>
      |                </th>
      |                <th scope="col" id="04" data-align="right">
      |                    <p>1 poeng</p>
      |                </th>
      |                <th scope="col" id="05" data-align="right">
      |                    <p>0 poeng</p>
      |                </th>
      |            </tr>
      |        </thead>
      |        <tbody>
      |            <tr>
      |                <th scope="row" id="r1">
      |                    <p>Grammatikk*</p>
      |                </th>
      |                <td headers="01 r1">---</td>
      |                <td headers="02 r1">Grammatikken er stort sett korrekt.</td>
      |                <td headers="03 r1">Grammatikken er delvis korrekt.</td>
      |                <td headers="04 r1">Grammatikken er ofte ukorrekt.</td>
      |                <td headers="05 r1">Grammatikken i besvarelsen ligger under det nivået som kan forventes på nivå II.</td>
      |            </tr>
      |            <tr>
      |                <th scope="row" id="r2">
      |                    <p>Setningsstruktur</p>
      |                </th>
      |                <td headers="01 r2">Setningsstrukturen er korrekt. </td>
      |                <td headers="02 r2">Setningsstrukturen er stort sett korrekt.</td>
      |                <td headers="03 r2">Flere setninger har ukorrekt struktur.</td>
      |                <td headers="04 r2">Mange setninger har ukorrekt struktur.</td>
      |                <td headers="05 r2">Setningsstrukturen i besvarelsen ligger under det nivået som kan forventes på nivå II.</td>
      |            </tr>
      |            <tr>
      |                <th scope="row" id="r3">
      |                    <p>Vokabular</p>
      |                </th>
      |                <td headers="01 r3">Vokabularet er variert og passer godt inn i sammenhengen.**</td>
      |                <td headers="02 r3">---</td>
      |                <td headers="03 r3">Vokabularet er mindre variert og/eller passer ikke alltid godt inn i sammenhengen.</td>
      |                <td headers="04 r3">Vokabularet er upresist og passer ofte dårlig inn i sammenhengen.</td>
      |                <td headers="05 r3">Vokabularet i besvarelsen ligger under det nivået som kan forventes på nivå II.</td>
      |            </tr>
      |            <tr>
      |                <th scope="row" id="r4">
      |                    <p>Rettskriving</p>
      |                </th>
      |                <td headers="01 r4">---</td>
      |                <td headers="02 r4">---</td>
      |                <td headers="03 r4">Rettskrivingen er korrekt.</td>
      |                <td headers="04 r4">Rettskrivingen inneholder få feil.</td>
      |                <td headers="05 r4">Rettskrivingen i besvarelsen ligger under det som kan forventes på nivå II.</td>
      |            </tr>
      |            <tr>
      |                <th scope="row" id="r5">
      |                    <p>Språkhandlinger</p>
      |                </th>
      |                <td headers="01 r5">Informerer, forteller, beskriver og begrunner på en selvstendig måte.</td>
      |                <td headers="02 r5">Informerer, forteller, beskriver og begrunner på en god måte.</td>
      |                <td headers="03 r5">Informerer, forteller, beskriver og begrunner på en enkel måte.</td>
      |                <td headers="04 r5">Informerer, forteller, beskriver og begrunner på en svært enkel måte.</td>
      |                <td headers="05 r5">Svaret gir ikke tilstrekkelig grunnlag for å vurdere språkhandlinger.</td>
      |            </tr>
      |        </tbody>
      |    </table>
      |    <p><em>* Med grammatikk menes for eksempel verbbruk i person og tid som er relevant med tanke på oppgavene og nivå, adjektiv, kasus, </em>
      |        <ndlaembed data-content-id="8561" data-link-text="register" data-resource="concept" data-type="inline"></ndlaembed><em>, morfologi osv. </em>
      |    </p>
      |    <p><em>** For å få 4 poeng for vokabularet må begge kriterier oppfylles.</em></p>
      |    <h2>Eksempel på karaktersetting</h2>
      |    <p>Her kommer et eksempel på hvordan du vurderer eksamensoppgavene og regner ut sluttkarakteren.</p>
      |    <h3>Del 1: Lesing</h3>
      |    <p>En elev har oppnådd <strong>21 av 30 poeng</strong> i del 1. Ifølge tabell A får eleven altså karakteren 4 på lesedelen.</p>
      |    <h3>Del 2: Skrivedel</h3>
      |    <h4>Vurdering av hver enkelt oppgave</h4>
      |    <p>Først vurderer man hver av de tre skriveoppgave individuelt ved hjelp av tabell B.</p>
      |    <ul>
      |        <li>
      |            <p>Eleven får <strong>2 poeng</strong> i skriveoppgave 1. Teksten svarer på alt i oppgavebestillingen, men er usammenhengende og vanskelig å forstå.</p>
      |        </li>
      |        <li>
      |            <p>I skriveoppgave 2 har ikke eleven svart på alle deler av oppgaven. Teksten har også feil som gjør at den er delvis vanskelig å forstå. Eleven får da <strong>2 poeng</strong> på denne oppgaven.</p>
      |        </li>
      |        <li>
      |            <p>I skriveoppgave 3 svarer eleven på alle deler av oppgaven. Det er noen feil i teksten, men stort sett har den god flyt og er lett å forstå. Eleven får <strong>4 poeng</strong>.</p>
      |        </li>
      |    </ul>
      |    <p>Totalt får eleven altså <strong>8 av 15 mulige poeng</strong>.</p>
      |    <h4>Helhetsvurdering</h4>
      |    <p>Så bruker du tabell C for å vurdere elevens skrivekompetanse i sin helhet. Eleven i eksempelet vårt får <strong>2 poeng</strong> for grammatikk, <strong>3 poeng</strong> for setningsstruktur, <strong>2 poeng</strong> for vokabular, <strong>1 poeng </strong>for rettskriving og <strong>2 poeng</strong> for språkhandlinger.</p>
      |    <p>Totalt får eleven altså <strong>10 av 17 mulige poeng</strong>.</p>
      |    <h4>Karakter på del 2</h4>
      |    <p>Etter at alle oppgavene er vurdert, både individuelt og helhetlig, blir poengene for skrivedelen summert. Karakteren på denne delen settes ut fra en poengskala (se tabell D nedenfor). Eleven eksempelet vårt har fått <strong>18 av 32 mulige</strong> <strong>poeng </strong>og får dermed karakteren 3 på del 2.</p>
      |    <table>
      |        <caption>Tabell D: poengskala for Del 2</caption>
      |        <thead>
      |            <tr>
      |                <th scope="col">
      |                    <p>Sum av poeng for skriving</p>
      |                </th>
      |                <th scope="col">
      |                    <p>Karakter</p>
      |                </th>
      |            </tr>
      |        </thead>
      |        <tbody>
      |            <tr>
      |                <td data-align="center">30–32</td>
      |                <td data-align="center">6</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">25–29</td>
      |                <td data-align="center">5</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">20–24</td>
      |                <td data-align="center">4</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">14–19</td>
      |                <td data-align="center">3</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">10–13</td>
      |                <td data-align="center">2</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">0–9</td>
      |                <td data-align="center">1</td>
      |            </tr>
      |        </tbody>
      |    </table>
      |    <h3>Samla vurdering</h3>
      |    <p>For å regne ut samla karakter gjelder følgende regnestykke: <strong>karakter på del 1</strong>&#xa0;<strong>+</strong>&#xa0;<strong>karakter på del 2</strong>&#xa0;<strong>+</strong>&#xa0;<strong>karakter på del 2</strong>. Så bruker man tabellen nedenfor for å avgjøre den samla karakteren.</p>
      |    <p>Husk at begge delene må være bestått for å bestå hele eksamen. Om en elev for eksempel har fått karakteren 6 på del 1 og karakteren 1 på del 2, vurderes hele eksamen som ikke bestått.</p>
      |    <p>Eleven i eksempelet vårt har fått karakteren 4 på del 1 og karakteren 3 på del 2. Da blir regnestykket slikt: <strong>4</strong>&#xa0;<strong>+</strong>&#xa0;<strong>3</strong>&#xa0;<strong>+</strong>&#xa0;<strong>3</strong>&#xa0;<strong>=</strong>&#xa0;<strong>10</strong>. I tabell E ser vi at summen 10 tilsvarer karakter 3. Eleven får altså 3 som samla karakter på hele eksamen.</p>
      |    <table>
      |        <caption>tabell E: samla karakter på del 1 og del 2</caption>
      |        <thead>
      |            <tr>
      |                <th scope="col" data-align="center">
      |                    <p>Totalsum</p>
      |                </th>
      |                <th scope="col" data-align="center">
      |                    <p>Karakter</p>
      |                </th>
      |            </tr>
      |        </thead>
      |        <tbody>
      |            <tr>
      |                <td data-align="center">17–18</td>
      |                <td data-align="center">6</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">14–16</td>
      |                <td data-align="center">5</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">11–13</td>
      |                <td data-align="center">4</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">8–10</td>
      |                <td data-align="center">3</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">6–7</td>
      |                <td data-align="center">2</td>
      |            </tr>
      |            <tr>
      |                <td data-align="center">0–5</td>
      |                <td data-align="center">1</td>
      |            </tr>
      |        </tbody>
      |    </table>
      |    <details>
      |        <summary>
      |            <h2>Kilder</h2>
      |        </summary>
      |        <p>Utdanningsdirektoratet. (2024, 3. april). <em>Forberede og ta eksamen</em>. <a href="https://www.udir.no/eksamen-og-prover/eksamen/forberede-og-ta-eksamen/">https://www.udir.no/eksamen-og-prover/eksamen/forberede-og-ta-eksamen/ </a> </p> <p>Utdanningsdirektoratet. (u.å.). <em>Eksamensveiledning for fremmedspråk I og II, 2025</em>. Tilgjengelig via <a href="https://sokeresultat.udir.no/eksamensoppgaver.html?start=1&amp;query=fremmedspr%C3%A5k%20I%20og%20II&amp;ExKL=Kunnskapsl%C3%B8ftet%202020">https://sokeresultat.udir.no/eksamensoppgaver.html?start=1&amp;query=fremmedspr%C3%A5k%20I%20og%20II&amp;ExKL=Kunnskapsl%C3%B8ftet%202020 </a></p>
      |        <p>Utdanningsdirektoratet. (u.å.). <em>Veiledning for sensur av oppgaver i spansk I, II og III etter LK20, 2024</em>. Tilgjengelig via <em>Eksamensveiledning for fremmedspråk I og II, 2024</em> fra <a href="https://sokeresultat.udir.no/eksamensoppgaver.html?start=1&amp;query=fremmedspr%C3%A5k%20I%20og%20II&amp;ExKL=Kunnskapsl%C3%B8ftet%202020">https://sokeresultat.udir.no/eksamensoppgaver.html?start=1&amp;query=fremmedspr%C3%A5k%20I%20og%20II&amp;ExKL=Kunnskapsl%C3%B8ftet%202020</a></p>
      |	   </details>
      |</section>""".stripMargin

  test("should strip whitespace after period in content") {
    val doc = migration.convertContent(originalArticle, "nb")
    doc should equal(migratedArticle)
  }

}
