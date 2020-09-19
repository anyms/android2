dec = int(input("decimal > "))
start = 0
num = 2**start
decs = []
result = ""

while num <= dec:
    decs.insert(0, num)
    start = start + 1
    num = 2**start

for d in decs:
    if d <= dec:
        dec -= d
        result += "1"
    else:
        result += "0"

print("The binary is: {}".format(result))
