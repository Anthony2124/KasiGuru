import re

path = r'C:\KasiGuru\KasiGuru-main\app\src\main\java\com\kasiguru\data\local\DatabaseSeeder.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

body_words = {'arm', 'armpit', 'back', 'beard', 'belly', 'bile', 'blood', 'body', 'bone', 'brain', 'breast', 'buttocks', 'cheek', 'chest', 'chin', 'ear', 'earwax', 'elbow', 'eye', 'eyebrow', 'eyelash', 'face', 'finger', 'fingernail', 'foot', 'forehead', 'hair', 'hand', 'head', 'heart', 'heel', 'intestines', 'jaw', 'kidney', 'knee', 'leg', 'lip', 'liver', 'lung', 'mouth', 'muscle', 'nail', 'neck', 'nose', 'palm', 'rib', 'shoulder', 'skin', 'skull', 'sole', 'stomach', 'temple', 'thigh', 'throat', 'thumb', 'toe', 'tongue', 'tooth', 'vein', 'waist', 'wrist'}
animals_words = {'animal', 'ant', 'bird', 'butterfly', 'chick', 'chicken', 'cockroach', 'crab', 'crocodile', 'crow', 'deer', 'dog', 'eel', 'fish', 'fly', 'frog', 'goat', 'hawk', 'insect', 'lizard', 'monkey', 'mosquito', 'mouse', 'pig', 'rat', 'shark', 'shrimp', 'snake', 'spider', 'turtle', 'worm'}
food_words = {'bitter', 'coconut', 'egg', 'eggplant', 'food', 'fruit', 'honey', 'meat', 'milk', 'salt', 'sugar', 'sweet', 'water', 'rice', 'banana', 'cassava', 'taro', 'yam', 'drink', 'eat', 'cook'}
numbers_words = {'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'day', 'daytime', 'night', 'year', 'morning', 'afternoon', 'evening', 'month', 'week', 'today', 'tomorrow', 'yesterday'}
weather_words = {'cloud', 'cold', 'dew', 'dry', 'hot', 'lightning', 'moon', 'rain', 'sky', 'star', 'sun', 'thunder', 'weather', 'wind'}
nature_words = {'bamboo', 'bark', 'branch', 'earth', 'forest', 'leaf', 'mountain', 'ocean', 'river', 'sea', 'soil', 'stone', 'tree', 'wave'}
family_words = {'boy', 'brother-in-law', 'chief', 'child', 'cousin', 'father', 'girl', 'man', 'mother', 'person', 'relative', 'sister', 'uncle', 'woman', 'daughter', 'son', 'parent', 'husband', 'wife'}
emotions_words = {'anger', 'bad', 'beautiful', 'bright', 'clean', 'happy', 'sad', 'afraid', 'love', 'tired', 'sick', 'fear', 'joy', 'good'}
colors_words = {'black', 'blue', 'green', 'red', 'white', 'yellow', 'round', 'sharp', 'flat', 'long', 'short'}
tools_words = {'adze', 'arrow', 'axe', 'basket', 'blade', 'boat', 'charcoal', 'net', 'paddle', 'spear', 'trap'}

def get_category(eng):
    e = eng.lower().strip(',. ')
    if any(w in e for w in body_words): return 'Body Parts & Health'
    if any(w in e for w in animals_words): return 'Animals & Wildlife'
    if any(w in e for w in food_words): return 'Food & Dining'
    if any(w in e for w in numbers_words): return 'Numbers & Time'
    if any(w in e for w in weather_words): return 'Weather & Climate'
    if any(w in e for w in nature_words): return 'Nature & Environment'
    if any(w in e for w in family_words): return 'Family & People'
    if any(w in e for w in emotions_words): return 'Emotions & Feelings'
    if any(w in e for w in colors_words): return 'Colors & Shapes'
    if any(w in e for w in tools_words): return 'Occupations & Tools'
    return 'Greetings & Essentials'

def replace_block(m):
    eng = m.group(1)
    cat = get_category(eng)
    return f'english = "{eng}",\n            category = "{cat}"'

new_content = re.sub(r'english = "([^"]+)",\s*category = "[^"]+"', replace_block, content)

with open(path, 'w', encoding='utf-8') as f:
    f.write(new_content)

print('Successfully recategorized DatabaseSeeder.kt into 12 dedicated categories!')
