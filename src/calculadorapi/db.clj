(ns calculadorapi.db
    (:require
        [calculadorapi.conexoes :as conexoes]))

(defn traduzir [texto]
  (let [traducao (conexoes/traduzir texto "pt" "en")]
    (if (:error traducao)
      {:error "Erro ao traduzir"}
      (:traducao traducao))))

(def banco (atom []))

(defn limpar []
  (reset! banco []))

(defn perda? [registro]
  (= (:tipo registro) "perda"))

(defn calcular [acumulado registro]
  (let [valor (:valor registro)]
    (if (perda? registro)
      (- acumulado valor)
      (+ acumulado valor))))

(defn registros []
  @banco)

(defn registros-do-tipo [tipo]
  (filter #(= (:tipo %) tipo) (registros)))

(defn registros-do-dia [dia]
  (filter #(= (:data %) dia) (registros)))

(defn saldo []
  (reduce calcular 0 (registros)))

(defn saldo-do-dia [dia]
  (reduce calcular 0 (registros-do-dia dia)))

(defn registrar [registro]
  (let [colecao-nova (swap! banco conj registro)]
    (merge registro {:id (count colecao-nova)})))
  
(defn registrar-perda [registro]
  (let [registro-completo (merge registro {:valor (conexoes/pegar-gasto-calorias (traduzir (:descricao registro)))})]
    (registrar registro-completo)))

(defn registrar-ganho [registro]
  (let [registro-completo (merge registro {:valor (conexoes/pegar-ganho-calorias (traduzir (:descricao registro)))})]
    (registrar registro-completo)))

(defn novo-registro [registro]
  (let [tipo-registro (:tipo registro)]
    (if (= tipo-registro "perda")
      (registrar-perda registro)
      (registrar-ganho registro))))



