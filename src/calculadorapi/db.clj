(ns calculadorapi.db
    (:require
        [clojure.string :as str]
        [calculadorapi.conexoes :as conexoes]
        [java-time :as time]
        [cheshire.core :as json]))

(defn traduzir-pt-to-en [texto]
  (let [traducao (conexoes/traduzir texto "pt" "en")
        texto (first (:translations traducao))] 
    (if (:error traducao)
      {:error "Erro ao traduzir"}
      (:text texto))))

(defn traduzir-en-to-pt [texto]
  (let [traducao (conexoes/traduzir texto "en" "pt")
        texto (first (:translations traducao))]
    (if (:error traducao)
      {:error "Erro ao traduzir"}
      (:text texto))))

(def banco (atom []))

(def usuarios (atom []))

(defn registrar-usuario [registro]
  (let [peso-novo (* 2.2 (:peso registro))
        registro-atualizado (assoc registro :peso peso-novo)
        colecao-nova (swap! usuarios conj registro-atualizado)]
    (merge registro-atualizado {:id (count colecao-nova)})))

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


  
(defn pegar-exercicios [descricao]
  (let [descricao (traduzir-pt-to-en descricao)
        resposta (conexoes/pegar-atividades descricao)]
    (if (:error resposta)
      {:error "Erro ao pegar exercícios"}
      (let [exercicios (take 5 resposta)
            traduzidos (map (fn [x]
                              (let [nome-traduzido (traduzir-en-to-pt (:name x))]
                                (assoc x :name nome-traduzido)))
                            exercicios)]
        {:exercicios traduzidos}))))
  
(defn registrar-perda [registro index]
  (let [peso-usuario (:peso (first @usuarios)) 
        exercicio (traduzir-pt-to-en (:descricao registro))
        resposta (conexoes/pegar-gasto-calorias exercicio peso-usuario (:quantidade registro))
        calorias (nth resposta index) 
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

(defn pegar-alimentos [descricao]
  (let [resposta (conexoes/pegar-ganho-calorias descricao)]
    (if (:error resposta)
      {:error "Erro ao pegar alimentos"}
      (let [alimentos (take 5 resposta)]
        {:alimentos alimentos}))))

(defn registrar-ganho [registro index]
    (let [resposta (conexoes/pegar-ganho-calorias (:descricao registro))
          item (nth resposta index)
          calorias (extrair-calorias (:calorias item))
          peso (extrair-valor (:quantidade item))
          valor (/ (* calorias (:quantidade registro)) peso)]
    (if (:error resposta)
      {:error "Erro ao registrar ganho"}
      (registrar (merge registro {:valor valor})))
      
      ))

(defn novo-registro [registro]
  (let [tipo-registro (:tipo registro)
        index (:index registro)]
    (if (= tipo-registro "perda")
      (registrar-perda registro index)
      (registrar-ganho registro index))))





