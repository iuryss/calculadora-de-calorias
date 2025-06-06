(ns calculadorapi.db
    (:require
        [clojure.string :as str]
        [calculadorapi.conexoes :as conexoes]
        [java-time :as time]))

;; (defn traduzir [texto]
;;   (let [traducao (conexoes/traduzir texto "pt" "en")]
;;     (if (:error traducao)
;;       {:error "Erro ao traduzir"}
;;       (:traducao traducao))))

(def banco (atom []))

(def usuarios (atom []))

(defn registrar-usuario [registro]
  (let [colecao-nova (swap! usuarios conj registro)]
    (merge registro {:id (count colecao-nova)})))

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

(defn saldo []
  (reduce calcular 0 (registros)))

(defn br-para-iso [data-str]
  (let [formatter-br (time/formatter "dd/MM/yyyy")
        local-date   (time/local-date formatter-br data-str)]
    (time/format "yyyy-MM-dd" local-date)))

(defn registros-do-periodo [dinicio dfim]
  (let [inicio (time/local-date (br-para-iso dinicio))
        fim    (time/local-date (br-para-iso dfim))]
    (doall
     (filter (fn [registro]
               (let [data (time/local-date (br-para-iso (:data registro)))]
                 (and (not (.isBefore data inicio))
                      (not (.isAfter data fim)))))
             (registros)))))

(defn saldo-do-periodo [inicio fim]
  (reduce calcular 0 (registros-do-periodo inicio fim)))


(defn registrar [registro]
  (let [colecao-nova (swap! banco conj registro)]
    (merge registro {:id (count colecao-nova)})))
  
(defn registrar-perda [registro]
  (let [usuario (:peso (first @usuarios)) 
        resposta (conexoes/pegar-gasto-calorias (:descricao registro) usuario (:quantidade registro))
        calorias (first resposta) ;;ajustar para pegar a melhor opção ao usuario
        valor (/ (* (:quantidade registro) (:calories_per_hour calorias)) 60)]
    (if (:error resposta)
      {:error "Erro ao registrar perda"}
      (registrar (merge registro {:valor valor})))))


(defn extrair-valor[s]
  (when-let [match (re-find #"\((\d+)" s)]
    (Integer/parseInt (second match))))

(defn extrair-calorias [s]
  (let [partes (str/split s #" ")]
    (Integer/parseInt (first partes))))

(defn registrar-ganho [registro]
    (let [resposta (conexoes/pegar-ganho-calorias (:descricao registro))
        item (first resposta)
        calorias (extrair-calorias (:calorias item))
        peso (extrair-valor (:quantidade item))
        valor (/ (* calorias (:quantidade registro)) peso)];; ajustar para pegar a melhor opção ao usuario
    (if (:error resposta)
      {:error "Erro ao registrar ganho"}
      (registrar (merge registro {:valor valor})))))

(defn novo-registro [registro]
  (let [tipo-registro (:tipo registro)]
    (if (= tipo-registro "perda")
      (registrar-perda registro)
      (registrar-ganho registro))))



