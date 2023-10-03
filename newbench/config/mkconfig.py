import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt
def write_file(filepath, content):
    with open(filepath, "w") as f:
        f.write(content)

N0 = 10
def process(datapath, schema, header=None):
    with open(datapath, "r") as f:
        tble = [line.strip().split(",") for line in f.readlines() if line.strip()]
        if header: tble = tble[1:] # tpcds, no_comma has header, adjusted no 

    ret = []
    def discrete(idx, s):
        N = int(s[1:]) if len(s) > 1 else N0

        ds = ["\"" + row[idx].strip() + "\"" for row in tble if row[idx].strip()]
        ds = list(set(ds))
        ds = [d for d in ds if "�" not in d]
        if len(ds) > N: ds = ds[:N]
        return "Discrete({})".format(",".join(ds))
    def gaussian(idx, s, name=None):
        ds = [float(row[idx].strip()) for row in tble if row[idx].strip()]
        # ds = [int(float(row[idx].strip())) for row in tble if row[idx].strip()]

        L, R = None, None
        if ":" in s:
            rng = s.split(":")[1]
            if rng.split(",")[0].strip():
                #L = int(rng.split(",")[0])
                L = int(rng.split(",")[0])
            if rng.split(",")[1].strip():
                # R = int(rng.split(",")[1])
                R = int(rng.split(",")[1])
            if L is None: L = int(np.min(ds))
            if R is None: R = int(np.max(ds))
        
        mu, sig = np.mean(ds), np.std(ds)
        if name and name in ["cs_sales_price", "sr_return_amt", "ss_quantity", "ss_sales_price", "ss_ext_discount_amt", "ss_ext_sales_price", "ss_net_profit", \
                             "ws_ext_sales_price", "i_current_price", "cs_ext_sales_price"]:
            paras = {
                "cs_ext_sales_price": (5000, 3000), 
                "cs_sales_price": (400, 95), # good case
                "i_current_price": (60, 30),
                "ws_ext_sales_price": (3000, 3000),
                "ss_net_profit": (-800, 500),
                "ss_ext_sales_price": (500, 300),
                "ss_sales_price": (50, 40),
                "ss_quantity": (50, 28),
                "sr_return_amt": (1000, 1400), # bad case
                "ss_ext_discount_amt": (300, 200)
            }
            paras_ex = {
                "cs_sales_price": (0, 2000),
                "i_current_price": (0, 2000),
            }
            mu, sig = paras[name]
            if name in paras_ex:
                L, R = paras_ex[name]
            #L, R = None, None

            # sns.histplot(ds, kde=False).figure.savefig(f"plot/{name}.png")

        return ("{}<=".format(L) if L is not None else "") + \
                     "Gaussian({}, {})".format(mu, sig) + \
                     ("<={}".format(R) if R is not None else "")
    def uniform(idx, s):
        L, R = None, None
        if ":" in s:
            rng = s.split(":")[1]
            if rng.split(",")[0].strip():
                L = int(rng.split(",")[0])
            if rng.split(",")[1].strip():
                R = int(rng.split(",")[1])
        #ds = [int(row[idx].strip()) for row in tble if row[idx].strip()]
        ds = [float(row[idx].strip()) for row in tble if row[idx].strip()]
        if L is None: L = int(np.min(ds))
        if R is None: R = int(np.max(ds))
        return "Uniform({},{})".format(L, R)
        
    for idx in range(len(schema.split("|"))):
        s = schema.split("|")[idx]
        name = None if header is None else header[idx]
        if s[0] == "d": ret += [discrete(idx, s)]
        if s[0] == "g": ret += [gaussian(idx, s, name)]
        if s[0] == "u": ret += [uniform(idx, s)]
        # if s[0] == "t": ret += [minutime(idx, s)]
        if s[0] == "t": ret += ["DateTime"]
    #print("|".join(ret))
    
    if header:
        for col, a in zip(header, ret):
            print(" ", col, ":", a)
    return "|".join(ret)

def processAll(dpath, dschema):
    ret = []
    for i in range(len(dpath)):
        ret += ["input{}:={}".format(i+1, process(dpath[i], dschema[i]))]
    return "\n".join(ret) + "\n"
    
def movie1():
    s_movie1 = "d|u:1905,2022|d|g:0,10|d"
    
    ret = processAll(["../src/movie1/data/input{}.csv".format(i+1) for i in range(1)], \
                     [s_movie1])
    write_file("movie1.config", ret)
def usedcars():
    s_usedcars_1 = "d|d"
    s_usedcars_2 = "d|u|g:,|g:,|d|g:,"
    
    ret = processAll(["../src/usedcars/data/input{}.csv".format(i+1) for i in range(2)], \
                     [s_usedcars_1, s_usedcars_2])
    write_file("usedcars.config", ret)
def credit():
    s_credit_1 = "u|g:,|d|d"
    
    ret = processAll(["../src/credit/data/input{}.csv".format(i+1) for i in range(1)], \
                     [s_credit_1])
    write_file("credit.config", ret)
def airport():
    s_airport_1 = "d|g:0,|d|d|d" # "d|g:0,|d|g:-90,90|g:-90,90"
    s_airport_2 = "d|d"
    
    ret = processAll(["../src/airport/data/input{}.csv".format(i+1) for i in range(2)], \
                     [s_airport_1, s_airport_2])
    write_file("airport.config", ret)

def transit():
    s_transit_1 = "d|d|d|d|d"
    # s_transit_1 = "d|d|t|t|d"
    
    ret = processAll(["../src/transit/data/input{}.csv".format(i+1) for i in range(1)], \
                     [s_transit_1])
    write_file("transit.config", ret)
movie1()
usedcars()
credit()
airport()
transit()

########## tpc-ds ###########
tables = ["store_returns", "date_dim", "store", "customer", "store_sales", "item", "customer_address",
  "customer_demographics", "promotion", "web_sales", "catalog_sales"]

table_used = {
    "Q1": ["store_returns", "date_dim", "store", "customer"],
    "Q3": ["store_sales", "date_dim", "item"],
    "Q6": ["customer_address", "customer", "store_sales", "date_dim", "item"],
    "Q7": ["customer_demographics", "promotion", "store_sales", "date_dim", "item"],
    "Q12": ["web_sales", "date_dim", "item"],
    "Q15": ["catalog_sales", "customer", "customer_address", "date_dim"],
    "Q19": ["date_dim", "store_sales", "item", "customer", "customer_address", "store"],
    "Q20": ["catalog_sales", "date_dim", "item"],
}

def config_tpcds():
    columns = dict()
    table2cols = dict()
    with open("./tpcds/schemas.txt", "r") as f:
        for line in f.readlines():
            if ":" not in line: 
                if line.strip():
                    table_name = line.strip()
                    if table_name not in table2cols:
                        table2cols[table_name] = []
            else:
                k = line.split(":")[0].split(" ")[-1].strip()
                v = line.split(":")[1].strip()
                columns[k] = v
                table2cols[table_name].append(k)

    schema = dict()
    for table_name, table_sch in table2cols.items():
        if table_name == "root": continue
        data_path = "./tpcds/nocomma_tpcds/{}.csv".format(table_name)
        data_path2 = "./tpcds/adjusted_data/{}.csv".format(table_name)
        print(table_name)
        with open(data_path, "r") as f:
            header = f.readline().strip().split(",")
            
            ret = []
            for c in header:
                t = columns[c].strip().split(" ")[0]
                if ("_sk" in c) or ("_id" in c) or t == "string":
                    ret += ["d"]
                elif c in ["p_response_target", "sr_ticket_number", "s_floor_space", "cs_order_number", \
                    "ss_ticket_number", "ws_order_number"]:
                    ret += ["d"]
                elif ("_date" in c):
                    ret += ["d"]
                elif c.startswith("d_") or "_birth_" in c: # date_
                    ret += ["d"]
                elif c == "s_tax_precentage" or c == "s_gmt_offset" or c == "p_cost": # gaussian(0,0)
                    ret += ["d"]
                elif t.startswith("decimal"):
                    ret += ["g:,"]
                elif c.endswith("_quantity") or c.endswith("_count"):
                    ret += ["g:,"]
                elif c in ["s_number_employees", "cd_purchase_estimate"]:
                    ret += ["g:,"]
                else:
                    print(c, t)
            
        schema[table_name] = process(data_path2, "|".join(ret), header)
        # print(schema[table_name])
    #print(columns)

    for bench_name, tables in table_used.items():
        ret = []
        for i in range(len(tables)):
            ret += ["input{}:={}".format(i+1, schema[tables[i]])]
        write_file("./{}.config".format(bench_name), "\n".join(ret) + "\n")


config_tpcds()